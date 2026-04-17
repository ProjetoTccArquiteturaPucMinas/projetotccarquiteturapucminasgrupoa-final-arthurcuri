package com.example.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {
    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {
        List<ItemCarrinho> itens = new ArrayList<>();
        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }
        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = (BigDecimal) itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // =========================
        // Calcula percentual de desconto
        // =========================
        BigDecimal percentualDesconto = calcularPercentualDesconto(selecoes);
        // =========================
        // Calcula valor do desconto
        // =========================
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto).divide(BigDecimal.valueOf(100));
        // =========================
        // Calcula total
        // =========================
        BigDecimal total = subtotal.subtract(valorDesconto);
        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    public BigDecimal calcularPercentualDesconto(List<SelecaoCarrinho> selecoes) { 
        int quantidadeTotal = selecoes.stream()
                .mapToInt(SelecaoCarrinho::getQuantidade)
                .sum();

        BigDecimal percentualDesconto = BigDecimal.ZERO;
        // Desconto por quantidade total de itens
        if (quantidadeTotal == 2) {
            percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(5));
        } else if (quantidadeTotal == 3) {
            percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(7));
        } else if (quantidadeTotal >= 4) {
            percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(10));
        }
        // Desconto adicional por categoria        
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            switch (produto.getCategoria()) {
                case CAPINHA:
                    percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(3));
                    break;
                case CARREGADOR:
                    percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(5));
                    break;
                case FONE:
                    percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(3));
                    break;
                case PELICULA:
                    percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(2));
                    break;
                case SUPORTE:
                    percentualDesconto = percentualDesconto.add(BigDecimal.valueOf(2));
                    break;
            }
        }
        return percentualDesconto;
    }
}
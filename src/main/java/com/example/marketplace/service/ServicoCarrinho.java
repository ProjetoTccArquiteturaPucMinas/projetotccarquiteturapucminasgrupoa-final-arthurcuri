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

    private static final BigDecimal DESCONTO_MAXIMO = BigDecimal.valueOf(25);

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {
        List<ItemCarrinho> itens = new ArrayList<>();

        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produto não encontrado: " + selecao.getProdutoId()));
            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualDesconto = calcularPercentualDesconto(selecoes);
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    private BigDecimal calcularPercentualDesconto(List<SelecaoCarrinho> selecoes) {
        int quantidadeTotal = selecoes.stream().mapToInt(SelecaoCarrinho::getQuantidade).sum();

        BigDecimal descontoQuantidade = BigDecimal.ZERO;
        if (quantidadeTotal == 2) {
            descontoQuantidade = BigDecimal.valueOf(5);
        } else if (quantidadeTotal == 3) {
            descontoQuantidade = BigDecimal.valueOf(7);
        } else if (quantidadeTotal >= 4) {
            descontoQuantidade = BigDecimal.valueOf(10);
        }

        BigDecimal descontoCategoria = BigDecimal.ZERO;
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produto não encontrado: " + selecao.getProdutoId()));

            BigDecimal percentualCategoria = descontoPorCategoria(produto.getCategoria().toString());
            // multiplica pela quantidade pois cada unidade conta (Cenário 8, 9, 11)
            descontoCategoria = descontoCategoria.add(
                    percentualCategoria.multiply(BigDecimal.valueOf(selecao.getQuantidade())));
        }

        BigDecimal descontoTotal = descontoQuantidade.add(descontoCategoria);

        // aplica teto de 25% (Cenários 7 e 11)
        return descontoTotal.min(DESCONTO_MAXIMO);
    }

    private BigDecimal descontoPorCategoria(String categoria) {
        return switch (categoria) {
            case "CAPINHA"    -> BigDecimal.valueOf(3);
            case "CARREGADOR" -> BigDecimal.valueOf(5);
            case "FONE"       -> BigDecimal.valueOf(3);
            case "PELICULA"   -> BigDecimal.valueOf(2);
            case "SUPORTE"    -> BigDecimal.valueOf(2);
            default           -> BigDecimal.ZERO;
        };
    }
}
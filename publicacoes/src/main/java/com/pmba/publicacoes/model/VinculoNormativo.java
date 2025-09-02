package com.pmba.publicacoes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // Importe esta classe
import lombok.ToString; // Importe esta classe

@Data
@EqualsAndHashCode(exclude = {"publicacaoOrigem", "publicacaoDestino"}) // <-- ADICIONE ESTA LINHA
@ToString(exclude = {"publicacaoOrigem", "publicacaoDestino"}) // <-- ADICIONE ESTA LINHA
@Entity
public class VinculoNormativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento: Muitos Vínculos podem apontar para UMA Publicação de origem.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_origem_id", nullable = false)
    private Publicacao publicacaoOrigem; // O documento que está sendo alterado

    // Relacionamento: Muitos Vínculos podem ser criados por UMA Publicação de destino.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_destino_id", nullable = false)
    private Publicacao publicacaoDestino; // O documento que efetua a alteração

    @Enumerated(EnumType.STRING) // Grava o nome do Enum (REVOGA) em vez de um número (0).
    private TipoVinculo tipoVinculo;

    // No futuro, o frontend vai gerar um ID para um trecho específico do HTML.
    // Por enquanto, vamos deixar um texto simples.
    private String seletorTrecho;

    private String textoDoTrecho;
}
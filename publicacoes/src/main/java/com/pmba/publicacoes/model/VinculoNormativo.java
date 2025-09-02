package com.pmba.publicacoes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = {"publicacaoOrigem", "publicacaoDestino"})
@ToString(exclude = {"publicacaoOrigem", "publicacaoDestino"})
@Entity
public class VinculoNormativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_origem_id", nullable = false)
    private Publicacao publicacaoOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_destino_id", nullable = false)
    private Publicacao publicacaoDestino;

    @Enumerated(EnumType.STRING)
    private TipoVinculo tipoVinculo;

    private String seletorTrecho;

    @Column(columnDefinition = "TEXT")
    private String textoDoTrecho;

    // =================================================================
    // NOVO CAMPO PARA GUARDAR O TEXTO DA ALTERAÇÃO
    // =================================================================
    @Column(columnDefinition = "TEXT")
    private String textoNovo;
    // =================================================================
}


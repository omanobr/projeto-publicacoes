package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.TipoVinculo;
import lombok.Data;

@Data
public class VinculoResponseDTO {
    private Long id;
    private Long publicacaoOrigemId;
    private String publicacaoOrigemTitulo;
    private Long publicacaoDestinoId;
    private String publicacaoDestinoTitulo;
    private TipoVinculo tipoVinculo;
    private String textoDoTrecho;
}
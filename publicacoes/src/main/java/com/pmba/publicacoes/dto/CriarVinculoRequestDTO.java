package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.TipoVinculo;
import lombok.Data;

@Data
public class CriarVinculoRequestDTO {
    private Long publicacaoOrigemId;
    private Long publicacaoDestinoId;
    private TipoVinculo tipoVinculo;
    private String textoDoTrecho;
}
package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.TipoVinculo;
import lombok.Data;

@Data
public class VinculoSimpleDTO {
    private Long id;
    private TipoVinculo tipoVinculo;
    private String textoDoTrecho;
}
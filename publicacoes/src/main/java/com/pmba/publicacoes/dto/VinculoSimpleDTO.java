package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.TipoVinculo;
import lombok.Data;

/**
 * DTO simplificado para representar um vínculo nas listas do painel de gestão.
 */
@Data
public class VinculoSimpleDTO {
    private Long id;
    private TipoVinculo tipoVinculo;
    private String textoDoTrecho;

    // Campos adicionados para fornecer contexto no painel
    private Long publicacaoOrigemId;
    private String publicacaoOrigemTitulo;
    private Long publicacaoDestinoId;
    private String publicacaoDestinoTitulo;
}

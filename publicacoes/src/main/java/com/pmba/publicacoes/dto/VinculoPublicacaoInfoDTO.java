package com.pmba.publicacoes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simples para carregar informações básicas de uma publicação vinculada.
 * Usado para evitar carregar o objeto Publicacao inteiro na resposta da API.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VinculoPublicacaoInfoDTO {
    private Long id;
    private String titulo;
}

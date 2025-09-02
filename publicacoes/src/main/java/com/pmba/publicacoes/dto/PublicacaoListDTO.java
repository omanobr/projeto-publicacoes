package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PublicacaoListDTO {
    private Long id;
    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private StatusPublicacao status; // Mantemos o status para a tag "REVOGADO"

    // O campo problemático 'private Set<VinculoSimpleDTO> vinculosGerados;' FOI REMOVIDO.
}
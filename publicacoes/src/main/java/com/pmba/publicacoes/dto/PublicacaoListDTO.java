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
    private StatusPublicacao status;

    // O campo 'vinculosGerados' foi removido para eliminar a complexidade e a fonte do erro.
}

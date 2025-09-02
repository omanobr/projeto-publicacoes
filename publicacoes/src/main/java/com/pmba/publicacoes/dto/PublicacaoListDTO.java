package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.AllArgsConstructor; // Adicione esta linha
import lombok.Data;
import lombok.NoArgsConstructor; // Adicione esta linha

import java.time.LocalDate;

@Data
@NoArgsConstructor      // Adicione esta anotação
@AllArgsConstructor     // Adicione esta anotação
public class PublicacaoListDTO {
    private Long id;
    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private StatusPublicacao status;
}
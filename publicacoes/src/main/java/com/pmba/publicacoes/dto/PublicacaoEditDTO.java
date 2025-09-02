package com.pmba.publicacoes.dto;

import com.pmba.publicacoes.model.StatusPublicacao;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO (Data Transfer Object) projetado especificamente para a página de edição.
 * Contém não apenas os dados da publicação em si, mas também as listas de
 * vínculos que ela cria e recebe, para serem exibidas no painel de gestão.
 */
@Data
public class PublicacaoEditDTO {

    // Dados da publicação que está a ser editada
    private Long id;
    private String titulo;
    private String numero;
    private String tipo;
    private LocalDate dataPublicacao;
    private String conteudoHtml; // O conteúdo "limpo", sem processamento de vínculos
    private StatusPublicacao status;

    // Lista de vínculos que ESTA publicação cria (ela é a ORIGEM)
    private List<VinculoSimpleDTO> vinculosGerados;

    // Lista de vínculos que ESTA publicação recebe (ela é o DESTINO)
    private List<VinculoSimpleDTO> vinculosRecebidos;
}


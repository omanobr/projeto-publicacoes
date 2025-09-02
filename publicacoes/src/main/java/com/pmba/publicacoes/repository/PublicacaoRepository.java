package com.pmba.publicacoes.repository;

import com.pmba.publicacoes.dto.PublicacaoListDTO; // Adicione esta linha
import com.pmba.publicacoes.model.Publicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface PublicacaoRepository extends JpaRepository<Publicacao, Long> {

    // VVV--- ADICIONE ESTE NOVO MÉTODO ---VVV
    @Query("SELECT new com.pmba.publicacoes.dto.PublicacaoListDTO(p.id, p.titulo, p.numero, p.tipo, p.dataPublicacao, p.status) FROM Publicacao p")
    List<PublicacaoListDTO> findAllForListView();
    // ^^^--- FIM DO NOVO MÉTODO ---^^^


    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO publicacao (titulo, numero, tipo, data_publicacao, conteudo_html, status) " +
                    "VALUES (:titulo, :numero, :tipo, :dataPublicacao, :conteudoHtml, 'ATIVA')",
            nativeQuery = true
    )
    void criarNovaPublicacao(
            @Param("titulo") String titulo,
            @Param("numero") String numero,
            @Param("tipo") String tipo,
            @Param("dataPublicacao") LocalDate dataPublicacao,
            @Param("conteudoHtml") String conteudoHtml
    );
}
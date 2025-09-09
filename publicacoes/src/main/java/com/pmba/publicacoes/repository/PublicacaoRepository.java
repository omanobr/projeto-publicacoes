package com.pmba.publicacoes.repository;

import com.pmba.publicacoes.model.Publicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// VVV--- ADICIONE JpaSpecificationExecutor ---VVV
public interface PublicacaoRepository extends JpaRepository<Publicacao, Long>, JpaSpecificationExecutor<Publicacao> {

    // Este método não é mais necessário pois a busca agora é feita via Specification
    // @Query(...)
    // List<PublicacaoListDTO> findAllForListView();


    @Modifying
    @Transactional
    @Query(
            value = "INSERT INTO publicacao (titulo, numero, tipo, data_publicacao, conteudo_html, status, bgo) " +
                    "VALUES (:titulo, :numero, :tipo, :dataPublicacao, :conteudoHtml, 'ATIVA', :bgo)",
            nativeQuery = true
    )
    void criarNovaPublicacao(
            @Param("titulo") String titulo,
            @Param("numero") String numero,
            @Param("tipo") String tipo,
            @Param("dataPublicacao") LocalDate dataPublicacao,
            @Param("conteudoHtml") String conteudoHtml,
            @Param("bgo") String bgo
    );
}


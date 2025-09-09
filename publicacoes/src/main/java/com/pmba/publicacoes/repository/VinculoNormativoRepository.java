package com.pmba.publicacoes.repository;

import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface VinculoNormativoRepository extends JpaRepository<VinculoNormativo, Long> {

    List<VinculoNormativo> findAllByPublicacaoDestinoId(Long publicacaoId);

    List<VinculoNormativo> findAllByPublicacaoOrigemId(Long publicacaoId);

    @Query("SELECT DISTINCT vn.publicacaoDestino.id FROM VinculoNormativo vn WHERE vn.publicacaoDestino.id IN :destinoIds AND vn.tipoVinculo IN :tipos")
    Set<Long> findAlteredDestinoIds(@Param("destinoIds") List<Long> destinoIds, @Param("tipos") Set<TipoVinculo> tipos);

}


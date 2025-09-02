package com.pmba.publicacoes.repository;

import com.pmba.publicacoes.model.VinculoNormativo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // Adicione este import

public interface VinculoNormativoRepository extends JpaRepository<VinculoNormativo, Long> {

    // ADICIONE APENAS ESTA LINHA
    List<VinculoNormativo> findAllByPublicacaoDestinoId(Long publicacaoId);

}
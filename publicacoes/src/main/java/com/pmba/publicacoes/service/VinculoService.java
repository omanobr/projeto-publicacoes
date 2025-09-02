package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.VinculoResponseDTO;
import com.pmba.publicacoes.model.Publicacao;
import com.pmba.publicacoes.model.StatusPublicacao;
import com.pmba.publicacoes.model.TipoVinculo;
import com.pmba.publicacoes.model.VinculoNormativo;
import com.pmba.publicacoes.repository.PublicacaoRepository;
import com.pmba.publicacoes.repository.VinculoNormativoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VinculoService {

    @Autowired
    private VinculoNormativoRepository vinculoRepository;
    @Autowired
    private PublicacaoRepository publicacaoRepository;

    @Transactional
    public VinculoResponseDTO criarVinculo(Long publicacaoOrigemId, Long publicacaoDestinoId, TipoVinculo tipoVinculo, String textoDoTrecho, String textoNovo) {
        Publicacao origem = publicacaoRepository.findById(publicacaoOrigemId)
                .orElseThrow(() -> new RuntimeException("Publicação de origem não encontrada com id: " + publicacaoOrigemId));
        Publicacao destino = publicacaoRepository.findById(publicacaoDestinoId)
                .orElseThrow(() -> new RuntimeException("Publicação de destino não encontrada com id: " + publicacaoDestinoId));

        VinculoNormativo novoVinculo = new VinculoNormativo();
        novoVinculo.setPublicacaoOrigem(origem);
        novoVinculo.setPublicacaoDestino(destino);
        novoVinculo.setTipoVinculo(tipoVinculo);
        novoVinculo.setTextoDoTrecho(textoDoTrecho);
        novoVinculo.setTextoNovo(textoNovo); // Salva o novo texto

        VinculoNormativo salvo = vinculoRepository.save(novoVinculo);
        return convertToResponseDto(salvo);
    }

    @Transactional
    public VinculoResponseDTO revogarTotalmente(Long origemId, Long destinoId) {
        Publicacao origem = publicacaoRepository.findById(origemId)
                .orElseThrow(() -> new RuntimeException("Publicação de origem não encontrada com id: " + origemId));
        Publicacao destino = publicacaoRepository.findById(destinoId)
                .orElseThrow(() -> new RuntimeException("Publicação de destino não encontrada com id: " + destinoId));

        destino.setStatus(StatusPublicacao.REVOGADA);
        publicacaoRepository.save(destino);

        VinculoNormativo novoVinculo = new VinculoNormativo();
        novoVinculo.setPublicacaoOrigem(origem);
        novoVinculo.setPublicacaoDestino(destino);
        novoVinculo.setTipoVinculo(TipoVinculo.REVOGA);
        novoVinculo.setTextoDoTrecho("Revogação total do documento.");

        VinculoNormativo salvo = vinculoRepository.save(novoVinculo);
        return convertToResponseDto(salvo);
    }

    // NOVO MÉTODO PARA EXCLUIR UM VÍNCULO
    @Transactional
    public void excluirVinculo(Long vinculoId) {
        if (!vinculoRepository.existsById(vinculoId)) {
            throw new RuntimeException("Vínculo não encontrado com id: " + vinculoId);
        }
        vinculoRepository.deleteById(vinculoId);
    }

    private VinculoResponseDTO convertToResponseDto(VinculoNormativo vinculo) {
        VinculoResponseDTO dto = new VinculoResponseDTO();
        dto.setId(vinculo.getId());
        dto.setPublicacaoOrigemId(vinculo.getPublicacaoOrigem().getId());
        dto.setPublicacaoOrigemTitulo(vinculo.getPublicacaoOrigem().getTitulo());
        dto.setPublicacaoDestinoId(vinculo.getPublicacaoDestino().getId());
        dto.setPublicacaoDestinoTitulo(vinculo.getPublicacaoDestino().getTitulo());
        dto.setTipoVinculo(vinculo.getTipoVinculo());
        dto.setTextoDoTrecho(vinculo.getTextoDoTrecho());
        dto.setTextoNovo(vinculo.getTextoNovo());
        return dto;
    }
}


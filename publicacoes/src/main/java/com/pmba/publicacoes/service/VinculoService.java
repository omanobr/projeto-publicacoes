package com.pmba.publicacoes.service;

import com.pmba.publicacoes.dto.CriarVinculoRequestDTO;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VinculoService {

    @Autowired
    private VinculoNormativoRepository vinculoRepository;
    @Autowired
    private PublicacaoRepository publicacaoRepository;

    // Método principal que é chamado pela API para um único vínculo
    @Transactional
    public VinculoResponseDTO criarVinculo(Long publicacaoOrigemId, Long publicacaoDestinoId, TipoVinculo tipoVinculo, String textoDoTrecho, String textoNovo) {
        Publicacao destino = publicacaoRepository.findById(publicacaoDestinoId)
                .orElseThrow(() -> new RuntimeException("Publicação de destino não encontrada com id: " + publicacaoDestinoId));
        return criarVinculo(publicacaoOrigemId, destino, tipoVinculo, textoDoTrecho, textoNovo);
    }

    // Versão otimizada usada internamente
    @Transactional
    public VinculoResponseDTO criarVinculo(Long publicacaoOrigemId, Publicacao destino, TipoVinculo tipoVinculo, String textoDoTrecho, String textoNovo) {
        Publicacao origem = publicacaoRepository.findById(publicacaoOrigemId)
                .orElseThrow(() -> new RuntimeException("Publicação de origem não encontrada com id: " + publicacaoOrigemId));

        // Cria e salva o vínculo PRIMEIRO para obter o seu ID
        VinculoNormativo novoVinculo = new VinculoNormativo();
        novoVinculo.setPublicacaoOrigem(origem);
        novoVinculo.setPublicacaoDestino(destino);
        novoVinculo.setTipoVinculo(tipoVinculo);
        novoVinculo.setTextoDoTrecho(textoDoTrecho);
        novoVinculo.setTextoNovo(textoNovo);
        VinculoNormativo salvo = vinculoRepository.saveAndFlush(novoVinculo); // saveAndFlush para garantir que o ID seja gerado

        // VVV--- LÓGICA DE SUBSTITUIÇÃO APRIMORADA ---VVV
        if (tipoVinculo == TipoVinculo.ALTERA && textoNovo != null && !textoNovo.isEmpty()) {
            String conteudoAtual = destino.getConteudoHtml();
            // Cria a tag de inserção com o ID do vínculo
            String textoDeSubstituicao = String.format("<ins class=\"alteracao\" data-vinculo-id=\"%d\">%s</ins>", salvo.getId(), textoNovo);
            // Substitui o texto original pela nova tag
            String novoConteudo = conteudoAtual.replace(textoDoTrecho, textoDeSubstituicao);
            destino.setConteudoHtml(novoConteudo);
            publicacaoRepository.save(destino);
        }
        // ^^^--- FIM DA LÓGICA APRIMORADA ---^^^

        return convertToResponseDto(salvo);
    }

    @Transactional
    public void criarVinculosEmLote(List<CriarVinculoRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        Long publicacaoDestinoId = requests.get(0).getPublicacaoDestinoId();
        Publicacao destino = publicacaoRepository.findById(publicacaoDestinoId)
                .orElseThrow(() -> new RuntimeException("Publicação de destino não encontrada com id: " + publicacaoDestinoId));
        for (CriarVinculoRequestDTO request : requests) {
            criarVinculo(request.getPublicacaoOrigemId(), destino, request.getTipoVinculo(), request.getTextoDoTrecho(), request.getTextoNovo());
        }
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

    @Transactional
    public void excluirVinculo(Long vinculoId) {
        VinculoNormativo vinculoParaExcluir = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado com id: " + vinculoId));

        Publicacao publicacaoDestino = vinculoParaExcluir.getPublicacaoDestino();
        TipoVinculo tipoVinculo = vinculoParaExcluir.getTipoVinculo();

        if (tipoVinculo == TipoVinculo.ALTERA) {
            String conteudoAtual = publicacaoDestino.getConteudoHtml();
            String textoAntigo = vinculoParaExcluir.getTextoDoTrecho();
            String textoNovo = vinculoParaExcluir.getTextoNovo();
            // Monta a tag exata que foi inserida para garantir a remoção correta
            String htmlParaRemover = String.format("<ins class=\"alteracao\" data-vinculo-id=\"%d\">%s</ins>", vinculoParaExcluir.getId(), textoNovo);
            String conteudoRestaurado = conteudoAtual.replace(htmlParaRemover, textoAntigo);
            publicacaoDestino.setConteudoHtml(conteudoRestaurado);
            publicacaoRepository.save(publicacaoDestino);

        } else if (tipoVinculo == TipoVinculo.ACRESCENTA) {
            String conteudoAtual = publicacaoDestino.getConteudoHtml();
            String textoAcrescentado = vinculoParaExcluir.getTextoNovo();
            String htmlParaRemover = String.format("<p class=\"paragrafo-acrescentado\"><ins>%s</ins></p>", textoAcrescentado);
            String conteudoRestaurado = conteudoAtual.replace(htmlParaRemover, "");
            publicacaoDestino.setConteudoHtml(conteudoRestaurado);
            publicacaoRepository.save(publicacaoDestino);
        }

        vinculoRepository.delete(vinculoParaExcluir);
        vinculoRepository.flush();

        if (tipoVinculo == TipoVinculo.REVOGA) {
            long outrosVinculosDeRevogacao = vinculoRepository.findAllByPublicacaoDestinoId(publicacaoDestino.getId())
                    .stream()
                    .filter(v -> v.getTipoVinculo() == TipoVinculo.REVOGA)
                    .count();
            if (outrosVinculosDeRevogacao == 0) {
                publicacaoDestino.setStatus(StatusPublicacao.ATIVA);
                publicacaoRepository.save(publicacaoDestino);
            }
        }
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
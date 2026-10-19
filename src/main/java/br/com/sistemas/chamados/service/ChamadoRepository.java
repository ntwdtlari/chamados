package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.AtualizarStatusRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;

    public ChamadoService(ChamadoRepository repository,
                          ClienteRepository clienteRepository,
                          CategoriaRepository categoriaRepository,
                          TecnicoRepository tecnicoRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + dto.clienteId() + " não encontrado"));

        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + dto.categoriaId() + " não encontrada"));

        Chamado chamado = new Chamado(dto.titulo(), dto.descricao(), dto.prioridade(), cliente, categoria);
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listar() {
        return repository.findAll().stream().map(ChamadoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atribuirTecnico(Long id, AtribuirTecnicoRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Não é possível atribuir técnico a um chamado fechado.");
        }

        Tecnico tecnico = tecnicoRepository.findById(dto.tecnicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico " + dto.tecnicoId() + " não encontrado"));

        chamado.setTecnico(tecnico);
        if (chamado.getStatus() == StatusChamado.ABERTO) {
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        }

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse atualizarStatus(Long id, AtualizarStatusRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Não é possível alterar o status de um chamado já fechado.");
        }

        chamado.setStatus(dto.status());
        if (dto.status() == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }

        return ChamadoResponse.de(chamado);
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }
}
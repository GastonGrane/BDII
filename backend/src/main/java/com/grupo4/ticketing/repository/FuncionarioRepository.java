package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, String> {}

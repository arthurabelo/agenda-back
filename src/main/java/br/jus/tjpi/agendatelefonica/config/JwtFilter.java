package br.jus.tjpi.agendatelefonica.config;

import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;

        // 1. Procura o cookie "jwt_token" na requisição que chegou do React
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        // 2. Se achou o token e ele for válido, autentica o usuário no contexto do Spring
        if (token != null && tokenService.isTokenValido(token)) {
            String username = tokenService.getUsernameFromToken(token);

            // Busca o usuário no banco para pegar a role correta dele
            // OBS: Se você tiver um método no UsuarioRepository como findByUsername, use-o aqui.
            // Se o seu método retornar Optional, não esqueça do .get() ou .orElse(null)
            Usuario usuario = repository.findByUsername(username).stream().findFirst().orElse(null);

            if (usuario != null) {
                // Converte a string "admin" em uma Autoridade que o Spring entende
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(usuario.getRole());

                // Cria o crachá de autenticação
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        usuario, null, Collections.singletonList(authority)
                );

                // Coloca o crachá no contexto de segurança (O Spring agora confia nesta requisição)
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 3. Deixa a requisição seguir o fluxo para o Controller
        filterChain.doFilter(request, response);
    }
}
package com.manacommunity.api.security;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.RolePermissionRepository;

import java.io.IOException;
import java.util.List;

@Component
public class MockJwtFilter extends OncePerRequestFilter {

    private final AppUserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public MockJwtFilter(AppUserRepository userRepository, RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        // Expecting format: mock-token-{userId}
        Long userId = null;
        if (token.startsWith("mock-token-")) {
            try {
                userId = Long.parseLong(token.substring(11));
            } catch (NumberFormatException ignored) {}
        } else if (token.equals("mock-jwt-token-12345") || token.equals("mock-jwt-token-67890")) {
            // Backwards compatibility for the old hardcoded tokens.
            // Assume user ID 1 (or any existing user) for testing.
            userId = 1L; 
        }

        if (userId != null) {
            AppUser user = userRepository.findById(userId).orElse(null);
            
            // If user 1 doesn't exist, try to find any ADMIN user to let the request pass
            if (user == null) {
                user = userRepository.findAll().stream().filter(u -> "ADMIN".equals(u.getRole())).findFirst().orElse(null);
                // Fallback to any user if no admin
                if (user == null) {
                    user = userRepository.findAll().stream().findFirst().orElse(null);
                }
            }

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<RolePermission> permissions = rolePermissionRepository.findByRoleIgnoreCase(user.getRole());
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                // Keep development safety/admin roles
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_AUCTION_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_AUCTION_AUCTIONEER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_AUCTION_TEAM_OWNER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
                
                // Add dynamic database permissions
                for (RolePermission perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm.getPermissionKey()));
                }
                
                UserPrincipal principal = new UserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        authorities
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}

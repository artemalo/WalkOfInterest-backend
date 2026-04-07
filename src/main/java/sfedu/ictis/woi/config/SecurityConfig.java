package sfedu.ictis.woi.config;

import org.springframework.context.annotation.Configuration;

//@Configuration
//@EnableWebSecurity
public class SecurityConfig {

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // Отключаем для REST, особенно если будет JWT
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/**").permitAll() // Разрешаем регистрацию и логин
//                        .anyRequest().authenticated() // Все остальное защищено
//                )
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Для будущего JWT
//                )
//                .httpBasic(Customizer.withDefaults()); // Пока оставим базовую аутентификацию для тестов
//
//        return http.build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(); // Обязательно шифруем пароли
//    }
}
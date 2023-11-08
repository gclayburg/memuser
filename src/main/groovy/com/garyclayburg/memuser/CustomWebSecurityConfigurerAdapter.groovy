package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

/**
 * <br><br>
 * Created 2018-03-05 21:57
 *
 * @author Gary Clayburg
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Profile('secure')
class CustomWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    public static final String USER = 'USER'

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .httpBasic().and().authorizeRequests()
                .antMatchers('/api/**').hasRole(USER)
                .antMatchers('/**').hasRole(USER).and()
                .csrf().disable().headers().frameOptions().disable()

//        http.authorizeRequests().antMatchers("/api")
//        .permitAll().anyRequest().authenticated()
    }

    @Bean
    @Override
    UserDetailsService userDetailsService() {
        def username = System.'MEMUSER_USERNAME'
        def password = System.'MEMUSER_PASSWORD'
        UserDetails user
        if (user && password) {
            user = User.withDefaultPasswordEncoder().username(username).password(password).roles(USER).build()
            log.info("Using basic authentiation with custom user and password")
        } else {
            user = User.withDefaultPasswordEncoder().username('user').password('passwordiswrong').roles(USER).build()
            log.info("Using basic authentication with standard user and password")
        }
        List<UserDetails> userList = [user]
        return new InMemoryUserDetailsManager(userList)
    }
}

@Slf4j
@Configuration
@Profile('insecure')
class Insecure extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
        log.info('insecure profile: security disabled')
    }
}

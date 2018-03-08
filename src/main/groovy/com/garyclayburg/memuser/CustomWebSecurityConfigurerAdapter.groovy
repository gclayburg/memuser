package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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
@Configuration
@EnableWebSecurity
@Profile("secure")
class CustomWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter{

    @Override
    protected void configure(HttpSecurity http) throws Exception{
        http.httpBasic().and().authorizeRequests()
                .antMatchers("/api/**").hasRole("USER")
                .antMatchers("/**").hasRole("USER").and()
        .csrf().disable().headers().frameOptions().disable()

//        http.authorizeRequests().antMatchers("/api")
//        .permitAll().anyRequest().authenticated()
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService(){
        UserDetails user = User.withUsername("user").password("password").roles("USER").build()
        List<UserDetails> userList = [user]
        return new InMemoryUserDetailsManager(userList)
    }

}

@Slf4j
@Configuration
@Profile("insecure")
class Insecure extends WebSecurityConfigurerAdapter{
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
        log.info("insecure profile: security disabled")
    }
}
package com.fredmaina.chatapp.Auth.services;

import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {


        @Autowired
        private UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            Optional <com.fredmaina.chatapp.Auth.Models.User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                throw new UsernameNotFoundException("User not found");
            }
            com.fredmaina.chatapp.Auth.Models.User user = userOptional.get();

            return User.builder()
                    .disabled(!user.isVerified())
                    .username(user.getEmail())
                    .password(user.getPassword() != null ? user.getPassword() : "DUMMY_PASSWORD")
                    .roles(String.valueOf(user.getRole()))
                    .build();
        }
    }

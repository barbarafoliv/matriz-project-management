package com.matriz.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

interface SignInAttemptRepository extends JpaRepository<SignInAttempt, String> {

}

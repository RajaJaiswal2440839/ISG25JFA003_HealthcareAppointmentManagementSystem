package com.cognizant.hams.controller;

import com.cognizant.hams.dto.request.AuthRequest;
import com.cognizant.hams.dto.response.AuthResponse;
import com.cognizant.hams.dto.response.PatientResponseDTO;
import com.cognizant.hams.entity.Patient;
import com.cognizant.hams.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;


    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authenticationRequest) {
        final String token = authService.createAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest registrationRequest) {

        Patient createdPatient=authService.registerNewUser(registrationRequest);
        PatientResponseDTO patientResponseDTO=new PatientResponseDTO();
        patientResponseDTO.setPatientId(createdPatient.getPatientId());
        patientResponseDTO.setName(createdPatient.getName());
        patientResponseDTO.setEmail(createdPatient.getEmail());
        patientResponseDTO.setAddress(createdPatient.getAddress());
        patientResponseDTO.setGender(createdPatient.getGender());
        patientResponseDTO.setBloodGroup(createdPatient.getBloodGroup());
        patientResponseDTO.setContactNumber(createdPatient.getContactNumber());
        patientResponseDTO.setDateOfBirth(createdPatient.getDateOfBirth());

        // This method now handles default role assignment internally.
        return ResponseEntity.ok("User registered successfully with ID: " + patientResponseDTO);
    }
}
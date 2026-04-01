package com.ims.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@ToString(exclude = "store")
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true)
    private String username;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false)
    private String password; 
	
    @Column(nullable = false,unique = true)
    private String email;
    
    @Column(nullable = false)
    private String role;
    
    @Column(nullable = false)
    private boolean active = true; 
    
    @ManyToOne
    @JoinColumn(name = "store_id")
    private StoreEntity store;
    
    private String otp; 
    private LocalDateTime otpExpiryTime;
}

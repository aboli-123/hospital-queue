package com.hospitalqueue.hospital_queue.domain;

import jakarta.persistence.*;

@Entity 
@Table( name="Patients")

public class Patient {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private String phone;

    protected Patient() {}

    public Patient(String name, String phone){
        this.name=name;
        this.phone=phone;

    }

    public Long getId()
    {
        return id;
    }
     
    public String getName(){
        return name;
    }

    public String getPhone(){
        return phone;
    }
    



    
}

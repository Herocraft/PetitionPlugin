package com.floyd.bukkit.petition.storage;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "pe_log")
public class PetitionLog
{
    private String message;

    public PetitionLog()
    {
        // TODO Auto-generated constructor stub
    }

    public PetitionLog(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return message;
    }
}

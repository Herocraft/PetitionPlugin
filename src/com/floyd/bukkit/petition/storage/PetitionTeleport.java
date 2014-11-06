package com.floyd.bukkit.petition.storage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "pe_teleport", uniqueConstraints = {})
public class PetitionTeleport
{
    @Id
    Long id;
    @Column(unique=true)
    private String playerName;
    @OneToOne
    private PetitionObject petition;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public PetitionObject getPetition()
    {
        return petition;
    }

    public void setPetition(PetitionObject petition)
    {
        this.petition = petition;
    }
}

package com.floyd.bukkit.petition.storage;

import org.bukkit.entity.Player;

import com.avaje.ebean.EbeanServer;

public class DbStorage implements Storage
{

    public DbStorage(EbeanServer database)
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public PetitionObject create(Long id, Player player, String newtitle)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PetitionObject load(Long petitionId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(PetitionObject petition)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void assign(PetitionObject petition, Player player, String name)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void unassign(PetitionObject petition, Player player)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(PetitionObject petition, Player player, String message)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void reopen(PetitionObject petition, Player player, String message)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void comment(PetitionObject petition, Player player, String message)
    {
        // TODO Auto-generated method stub

    }

}

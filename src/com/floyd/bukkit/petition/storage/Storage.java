package com.floyd.bukkit.petition.storage;

import java.util.HashMap;

import org.bukkit.entity.Player;

public interface Storage
{
    PetitionObject create(Long id, Player player, String newtitle);
    PetitionObject load(Long petitionId);
    void save(PetitionObject petition);
    void assign(PetitionObject petition, Player player, String name);
    void unassign(PetitionObject petition, Player player);
    void close(PetitionObject petition, Player player, String message);
    void reopen(PetitionObject petition, Player player, String message);
    void comment(PetitionObject petition, Player player, String message);
}

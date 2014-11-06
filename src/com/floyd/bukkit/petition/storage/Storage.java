package com.floyd.bukkit.petition.storage;

import java.util.List;

import org.bukkit.entity.Player;

public interface Storage
{
    PetitionObject create(Player player, String newtitle, String server);
    PetitionObject load(Long petitionId);
    List<PetitionObject> list(boolean isArchived, String filter);
    void assign(PetitionObject petition, Player player, String name);
    void unassign(PetitionObject petition, Player player);
    void close(PetitionObject petition, Player player, String message);
    void reopen(PetitionObject petition, Player player, String message);
    void comment(PetitionObject petition, Player player, String message);
}

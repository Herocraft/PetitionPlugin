package com.floyd.bukkit.petition.storage;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import com.avaje.ebean.EbeanServer;
import com.floyd.bukkit.petition.PetitionPlugin;

public class DbStorage implements Storage
{
    final EbeanServer database;

    public DbStorage(EbeanServer database)
    {
        this.database = database;
    }

    @Override
    public PetitionObject create(Player player, String title)
    {
        PetitionObject petition = database.createEntityBean(PetitionObject.class);
        petition.setTitle(title);
        if (player != null) {
            petition.setOwner(player.getName());
            petition.setLocation(player.getLocation());

        }
        database.save(petition);
        return petition;
    }

    @Override
    public PetitionObject load(Long petitionId)
    {
        return database.find(PetitionObject.class).where().eq("id", petitionId).findUnique();
    }

    @Override
    public List<PetitionObject> list(boolean isArchived, String filter)
    {
        List<PetitionObject> petitions = Collections.emptyList();
        if (StringUtils.isNotBlank(filter)) {
            filter = "%" + filter + "%";
            petitions = database.find(PetitionObject.class).where().eq("closed", isArchived).disjunction()
                    .add(database.getExpressionFactory().like("owner", filter)).disjunction().add(database.getExpressionFactory().like("assignee", filter))
                    .disjunction().add(database.getExpressionFactory().like("title", filter)).findList();
        }
        else {
            petitions = database.find(PetitionObject.class).where().eq("closed", isArchived).findList();
        }
        return petitions;
    }

    @Override
    public void assign(PetitionObject petition, Player player, String name)
    {
        String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
        PetitionComment comment = database.createEntityBean(PetitionComment.class);
        comment.setMessage("Assigned to " + name + " by " + moderator);
        petition.getLog().add(comment);
        petition.setAssignee(name);
        database.save(petition);
    }

    @Override
    public void unassign(PetitionObject petition, Player player)
    {
        String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
        PetitionComment comment = database.createEntityBean(PetitionComment.class);
        comment.setMessage("Unassigned by " + moderator);
        petition.getLog().add(comment);
        petition.setAssignee("*");
        database.save(petition);
    }

    @Override
    public void close(PetitionObject petition, Player player, String message)
    {
        String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
        PetitionComment comment = database.createEntityBean(PetitionComment.class);
        if (StringUtils.isEmpty(message)) {
            comment.setMessage("Closed by " + moderator);
        }
        else {
            comment.setMessage("Closed by " + moderator + ": " + message);
        }
        petition.getLog().add(comment);
        petition.setClosed(true);
        database.save(petition);
    }

    @Override
    public void reopen(PetitionObject petition, Player player, String message)
    {
        String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
        PetitionComment comment = database.createEntityBean(PetitionComment.class);
        if (StringUtils.isEmpty(message)) {
            comment.setMessage("Reopened by " + moderator);
        }
        else {
            comment.setMessage("Reopened by " + moderator + ": " + message);
        }
        petition.getLog().add(comment);
        petition.setClosed(false);
        database.save(petition);
    }

    @Override
    public void comment(PetitionObject petition, Player player, String message)
    {
        if (StringUtils.isNotBlank(message)) {
            String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
            PetitionComment comment = database.createEntityBean(PetitionComment.class);
            comment.setMessage(moderator + ": " + message);
            petition.getLog().add(comment);
            database.save(petition);
        }
    }

}

/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class BukkitScheduler extends AsyncScheduler {

    private final Plugin plugin;
    private final Executor syncExecutor;
    private final boolean isFolia;

    public BukkitScheduler(Plugin plugin, Logger logger) {
        super(logger, command -> {
            if (isFolia()) {
                FoliaAccessor.runAsync(plugin, command);
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, command);
            }
        });
        this.plugin = plugin;
        this.isFolia = isFolia();

        syncExecutor = this::runSync;
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public Executor getSyncExecutor() {
        return syncExecutor;
    }

    public void runSync(Runnable task) {
        if (isFolia) {
            FoliaAccessor.runSync(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runSync(Runnable task, Entity entity) {
        if (isFolia) {
            FoliaAccessor.runSync(plugin, task, entity);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runTaskLater(Runnable task, long delay) {
        if (isFolia) {
            FoliaAccessor.runTaskLater(plugin, task, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public void runTaskLater(Runnable task, long delay, Entity entity) {
        if (isFolia) {
            FoliaAccessor.runTaskLater(plugin, task, delay, entity);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public <T> Future<T> callSyncMethod(Callable<T> task) {
        if (isFolia) {
            return FoliaAccessor.callSyncMethod(plugin, task);
        } else {
            return Bukkit.getScheduler().callSyncMethod(plugin, task);
        }
    }

    public <T> Future<T> callSyncMethod(Callable<T> task, Entity entity) {
        if (isFolia) {
            return FoliaAccessor.callSyncMethod(plugin, task, entity);
        } else {
            return Bukkit.getScheduler().callSyncMethod(plugin, task);
        }
    }

    private static class FoliaAccessor {
        static void runAsync(Plugin plugin, Runnable task) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        }

        static void runSync(Plugin plugin, Runnable task) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        }

        static void runSync(Plugin plugin, Runnable task, Entity entity) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        }

        static void runTaskLater(Plugin plugin, Runnable task, long delay) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delay);
        }

        static void runTaskLater(Plugin plugin, Runnable task, long delay, Entity entity) {
            entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delay);
        }

        static <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }

        static <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task, Entity entity) {
            CompletableFuture<T> future = new CompletableFuture<>();
            entity.getScheduler().run(plugin, t -> {
                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }, null);
            return future;
        }
    }
}

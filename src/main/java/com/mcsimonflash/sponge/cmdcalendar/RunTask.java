package com.mcsimonflash.sponge.cmdcalendar;

import com.google.common.collect.Lists;

import com.mcsimonflash.sponge.cmdcalendar.objects.CmdCalTask;
import com.mcsimonflash.sponge.cmdcalendar.objects.CmdCalTask.TaskType;
import com.mcsimonflash.sponge.cmdcalendar.objects.CmdCalTask.TaskStatus;
import com.mcsimonflash.sponge.cmdcalendar.objects.Scheduler;
import com.mcsimonflash.sponge.cmdcalendar.objects.Interval;
import com.mcsimonflash.sponge.cmdcalendar.managers.Config;
import com.mcsimonflash.sponge.cmdcalendar.managers.Tasks;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RunTask {
    private static List<Task> activeTasks = Lists.newArrayList();

    static void setupTasks() {
        CmdCalendar.getPlugin().getLogger().info("CmdCal: Setting up tasks...");
        if (Config.isDisableRunTasksOnStartup()) {
            CmdCalendar.getPlugin().getLogger().info("Tasks not run: Config setting disableRunTasksOnStartup = true");
        } else {
            for (CmdCalTask cmdCalTask : Tasks.getTaskList()) {
                if (cmdCalTask.getStatus().equals(TaskStatus.Active)) {
                    addTask(cmdCalTask);
                }
            }
            if (!activeTasks.isEmpty()) {
                List<String> activatedTasks = Lists.newArrayList();
                for (Task task : activeTasks) {
                    activatedTasks.add(task.getName());
                }
                String activeTaskString = "Activated " + String.join(", ", activatedTasks) + ".";
                CmdCalendar.getPlugin().getLogger().info(activeTaskString);
            } else {
                CmdCalendar.getPlugin().getLogger().info("No tasks activated!");
            }
        }
    }

    public static boolean verifyTask(CmdCalTask cmdCalTask) {
        for (Task task : activeTasks) {
            if (task.getName().equals("CmdCal_RunTask_" + cmdCalTask.getName())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Task getTask(CmdCalTask cmdCalTask) {
        for (Task task : activeTasks) {
            if (task.getName().equals("CmdCal_RunTask_" + cmdCalTask.getName())) {
                return task;
            }
        }
        return null;
    }

    public static void addTask(CmdCalTask cmdCalTask) {
        if (cmdCalTask instanceof Scheduler) {
            if (cmdCalTask.getType().equals(TaskType.Scheduler)) {
                activeTasks.add(Task.builder().execute(
                        task -> {
                            String[] splitTaskSchedule = ((Scheduler) cmdCalTask).getSchedule().split(" ");
                            Calendar cal = Calendar.getInstance();
                            int[] calendarArray = {cal.get(Calendar.SECOND), cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)};

                            //SFZ: Implement Conditions;

                            boolean runTask = true;
                            for (int i = 0; i < 5; i++) {
                                if (!splitTaskSchedule[i].substring(0, 1).equals("*")) {
                                    if (splitTaskSchedule[i].substring(0, 1).equals("/")) {
                                        if (calendarArray[i] % Integer.parseInt(splitTaskSchedule[i].substring(1)) != 0) {
                                            runTask = false;
                                        }
                                    } else if (calendarArray[i] != Integer.parseInt(splitTaskSchedule[i])) {
                                        runTask = false;
                                    }
                                }
                            }

                            if (runTask) {
                                Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmdCalTask.getCommand());
                            }
                        })
                        .name("CmdCal_RunTask_" + cmdCalTask.getName())
                        .delay(1, TimeUnit.SECONDS)
                        .interval(1, TimeUnit.SECONDS)
                        .submit(CmdCalendar.getPlugin()));
            } else if (cmdCalTask.getType().equals(TaskType.GameTime)) {
                activeTasks.add(Task.builder().execute(
                        task -> {
                            //SFZ: Implement
                        })
                        .name("CmdCal_RunTask_" + cmdCalTask.getName())
                        .delayTicks(20)
                        .intervalTicks(20)
                        .submit(CmdCalendar.getPlugin()));
            }
        } else if (cmdCalTask instanceof Interval) {
            if (cmdCalTask.getType().equals(TaskType.Interval)) {
                activeTasks.add(Task.builder().execute(
                        task -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmdCalTask.getCommand()))
                        .name("CmdCal_RunTask_" + cmdCalTask.getName())
                        .delay(((Interval) cmdCalTask).getInterval(), Config.getTimeUnit())
                        .interval(((Interval) cmdCalTask).getInterval(), Config.getTimeUnit())
                        .submit(CmdCalendar.getPlugin()));
            } else if (cmdCalTask.getType().equals(TaskType.GameTick)) {
                activeTasks.add(Task.builder().execute(
                        task -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmdCalTask.getCommand()))
                        .name("CmdCal_RunTask_" + cmdCalTask.getName())
                        .delayTicks(((Interval) cmdCalTask).getInterval())
                        .intervalTicks(((Interval) cmdCalTask).getInterval())
                        .submit(CmdCalendar.getPlugin()));
            }
        }
    }

    public static boolean delTask(CmdCalTask cmdCalTask) {
        if (verifyTask(cmdCalTask)) {
            for (int i = 0; i < activeTasks.size(); i++) {
                if (activeTasks.get(i).getName().equalsIgnoreCase("CmdCal_RunTask_" + cmdCalTask.getName())) {
                    getTask(cmdCalTask).cancel();
                    activeTasks.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public static void delAll() {
        for (Task task : activeTasks) {
            Tasks.getTask(task.getName().replace("CmdCal_RunTask_", "")).setStatus(TaskStatus.Halted);
            task.cancel();
        }
    }
}
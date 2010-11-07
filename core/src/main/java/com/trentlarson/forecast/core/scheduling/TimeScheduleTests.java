package com.trentlarson.forecast.core.scheduling;

import java.io.PrintWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.trentlarson.forecast.core.dao.TeamHours;
import com.trentlarson.forecast.core.helper.ForecastUtil;

public class TimeScheduleTests {

  public static void main(String[] args) throws Exception {
    //unitMain(args);
    integrationMain(args);
  }
  
  
  
  private static final SimpleDateFormat SLASH_DATE = new SimpleDateFormat("yyyy/MM/dd");
  private static final SimpleDateFormat SLASH_TIME = new SimpleDateFormat("yyyy/MM/dd HH:mm");

  /**
   * This generates HTML output that can be compared with gantt-test.html
   */
  public static void unitMain(String[] args) throws Exception {

    //log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeSchedule.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);

    PrintWriter out = null;
    try {
      out = new PrintWriter(System.out);
      if (args.length > 0) {
        out = new PrintWriter(args[0]);
      }

      out.println("<P>");
      out.println("<H1>Here are the scheduling tests for team tasks.</H2>");

      // another test: allow start/end times outside 0-8 AM range
      // another test: I believe the lighter colors don't work for teams

      outputDaylightSavingsTestResults(out);

      outputNullTeamAndAssigneeTestResults(out);

      outputVariableTimeTestResults(out);

      outputSplitTeamTestResults(out);

      outputStartTimeTestResults(out);

      outputBlockedSubtaskTestResults(out);

      outputManyBlockedTestResults(out);

      out.println("<P>");
      out.println("<H1>Here are the basic TimeSchedule tests.</H2>");
      TimeSchedule.outputTestResults(out);

    } finally {
      try { out.close(); } catch (Exception e) {}
    }
  }

  /**
     A case with a very long task (which broke this).
   */
  public static void outputDaylightSavingsTestResults(PrintWriter out) throws Exception {
    
    out.println("<P>");
    out.println("<H2>... around a daylight-savings time switch.</H2>");

    String username = "matt";
    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-230", "~18-week issue", username, 1L, "1",
       (int) (751.5 * 3600), 0, null, null, 5, false)
    };
    
    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(1L, null, username, SLASH_TIME.parse("2006/12/03 00:00"), 40.0));
    hourList.add(new TeamHours(2L, null, username, SLASH_DATE.parse("2007/03/19"), 0.0));
    hourList.add(new TeamHours(3L, null, username, SLASH_TIME.parse("2007/03/19 04:00"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, username), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);

    Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "matt");

    // print out team table schedule
    List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

  }


    

  public static void outputNullTeamAndAssigneeTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks without a team or assignee.</H2>");

    out.println("I'm still debating whether this is a good idea.");

    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-231", "null team & assignee", null, null, null,
       8 * 3600, 0, null, null, 5, false)
    };
    
    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(1L, null, null, SLASH_TIME.parse("2006/12/03 00:00"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(null, null), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);

    Teams.AssigneeKey user = new Teams.AssigneeKey(null, null);

    // print out team table schedule
    List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

  }
    
  /**
     Now we test where a team may have issues to schedule, and it may have more
     (or fewer) than the standard 40 hours per week.
   */
  public static void outputVariableTimeTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for teams with various time availability.</H2>");

    Date startDate = SLASH_DATE.parse("2005/04/05");
    int jira_day = 8 * 60 * 60;

    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-200", "5-day issue", null, 1L, "1",
       5 * jira_day, 0 * jira_day,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-201", "3-day issue", null, 1L, "1",
       3 * jira_day, 0 * jira_day,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-202", "3 days again", null, 1L, "1",
       3 * jira_day, 0 * jira_day,
       SLASH_DATE.parse("2005/04/11"), null, 6, false)
      ,
      new IssueTree
      ("TEST-203", "7-day issue", null, 1L, "1",
       7 * jira_day, 0 * jira_day,
       null, null, 3, false)
      ,
      new IssueTree
      ("TEST-204", "7-day issue", null, 1L, "1",
       7 * jira_day, 0 * jira_day,
       null, null, 6, false)
    };

    
    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, null, SLASH_DATE.parse("2005/04/04"), 120.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, null), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);
      


    // print out team Gantt chart
    Teams.AssigneeKey user = new Teams.AssigneeKey(1L, null);
    out.println("<br><br>");
    out.println("Tree for " + user + " (shouldn't be longer than 2 weeks).<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));

    // print out team table schedule
    List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

  }

  public static void outputSplitTeamTestResults(PrintWriter out) throws Exception {
    
    out.println("<P>");
    out.println("<H2>... for people split across teams.</H2>");

    int jira_day = 8 * 60 * 60;


    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-205", "3-day issue", "trent", 1L, "1",
       3 * jira_day, 0 * jira_day,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-206", "3-day issue", "trent", 1L, "1",
       3 * jira_day, 0 * jira_day,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-207", "3-day issue", "trent", 2L, null,
       3 * jira_day, 0 * jira_day,
       null, null, 4, false)
      ,
      new IssueTree
      ("TEST-208", "3-day issue", "trent", null, null,
       3 * jira_day, 0 * jira_day,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-209", "3-day issue", "trent", null, null,
       3 * jira_day, 0 * jira_day,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-210", "3-day issue", "trent", null, null,
       3 * jira_day, 0 * jira_day,
       null, null, 7, false)
      ,
      new IssueTree
      ("TEST-211", "3-day issue", "trent", null, null,
       3 * jira_day, 0 * jira_day,
       null, null, 8, false)
      ,
      new IssueTree
      ("TEST-212", "3-day issue", "trent", null, null,
       3 * jira_day, 0 * jira_day,
       null, null, 9, false)
    };



    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();

    hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, "trent", SLASH_DATE.parse("2005/04/04"), 8.0));
    hourList.add(new TeamHours(1L, 1L, "trent", SLASH_DATE.parse("2005/04/18"), 16.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "trent"), hourList);
    hourList = new ArrayList();
    hourList.add(new TeamHours(2L, null, "trent", SLASH_DATE.parse("2005/04/04"), 8.0));
    hourList.add(new TeamHours(3L, null, "trent", SLASH_DATE.parse("2005/04/18"), 30.0));
    userWeeklyHours.put(new Teams.UserTimeKey(null, "trent"), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/11"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);

    out.println("<br><br>");
    out.println("Trees with hours on teams with hours: " + graph.getUserWeeklyHoursAvailable());
    out.println("<br><br>");

    Teams.AssigneeKey userKey = new Teams.AssigneeKey(1L, "trent");

    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, true, userKey.getUsername(), false, graph));



    // print out team table schedule for trent on team 1
    userKey = new Teams.AssigneeKey(1L, "trent");
    List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
    List<IssueTree> userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);


    // print out team table schedule for trent on team 2
    userKey = new Teams.AssigneeKey(2L, "trent");
    schedule = new ArrayList();
    userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

    // print out team table schedule for trent on no team
    userKey = new Teams.AssigneeKey(null, "trent");
    schedule = new ArrayList();
    userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);
  }




  public static void outputStartTimeTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks with a starting time.</H2>");

    Date startDate = SLASH_DATE.parse("2005/04/05");
    int jira_day = 8 * 60 * 60;

    IssueTree[] manyIssues =
      {
        new IssueTree
        ("TEST-100", "one week", "trent", 1L, "1",
         5 * jira_day, 0 * jira_day,
         null, null, 3, false)
        ,
        // This one has lower priority, but it should be scheduled first because
        // it has a start time.
        new IssueTree
        ("TEST-101", "one day", "trent", 1L, "1",
         1 * jira_day, 0 * jira_day,
         null, SLASH_DATE.parse("2005/04/11"), 6, false)
      };


    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, userWeeklyHours);
    List<TeamHours> hourList = new ArrayList();
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
        (userDetails, userWeeklyHours, sPrefs);



    {
      String userPerson = "trent";
      Long userTeam = 1L;
      Teams.AssigneeKey userKey = new Teams.AssigneeKey(userTeam, userPerson);

      // print out single-user table schedule
      List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(userKey);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);

      // print out single-user Gantt chart
      out.println("<br><br>");
      out.println("Tree for " + userKey + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences
         .createForUser(1, 0, true, false, false, userKey, false, graph));
    }

  }







  public static void outputBlockedSubtaskTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks blocked by a previous issue with a late subtask.</H2>");

    out.println("<br>");
    out.println("(TEST-22 should be blocked until the 21st.)");

    Date startDate = SLASH_DATE.parse("2005/04/05");

    IssueTree issue20 =
      new IssueTree
      ("TEST-20", "sub issue", "trent", 1L, "1", 16 * 3600, 0,
       null, null, 3, false);
    IssueTree issue21 =
      new IssueTree
      ("TEST-21", "some issue", "brent", 1L, "1", 32 * 3600, 0,
       null, null, 2, false);
    IssueTree issue22 =
      new IssueTree
      ("TEST-22", "some issue", "ken", 1L, "1", 1 * 3600, 0,
       null, null, 2, false);

    issue20.addSubtask(issue21);
    issue20.addDependent(issue22);

    IssueTree[] manyIssues = { issue20, issue21, issue22 };

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, new HashMap());

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, new HashMap(), sPrefs);

    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForIssues(1, 0, true, false, false,
                        new String[]{"TEST-22"},
                        false, graph));

  }

  public static void outputManyBlockedTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks with complicated dependencies.</H2>");

    out.println("<p>Creating issues without dependencies...<p>");

    Date startDate = SLASH_DATE.parse("2005/04/05");

    IssueTree issue_6 =
      new IssueTree
      ("TEST--6", "Ancestor test issue", "trent", 1L, "1",
       0 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue_5 =
      new IssueTree
      ("TEST--5", "Ancestor test issue", "trent", 1L, "1",
       4 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue_4 =
      new IssueTree
      ("TEST--4", "Ancestor test issue", "ken", 1L, "1",
       4 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue_3 =
      new IssueTree
      ("TEST--3", "Ancestor test issue", "brent", 1L, "1",
       4 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue_2 =
      new IssueTree
      ("TEST--2", "Ancestor test issue", "trent", 1L, "1",
       4 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue_1 =
      new IssueTree
      ("TEST--1", "Ancestor test issue", "ken", 1L, "1",
       4 * 3600, 0 * 3600, null, null, 1, false);
    IssueTree issue0 =
      new IssueTree
      ("TEST-0", "Grandparent test issue", "fred", 1L, "1",
       8 * 3600, 0 * 3600,
       SLASH_DATE.parse("2005/01/01"), null, 1, false);
    IssueTree issue1 =
      new IssueTree
      ("TEST-1", "Parent test issue", "brent", 1L, "1",
       10 * 3600, 0 * 3600,
       SLASH_DATE.parse("2005/04/01"), null, 1, false);
    IssueTree issue2 =
      new IssueTree
      ("TEST-2", "issue", "ken", 1L, "1", 24 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/16"), null, 8, false);
    IssueTree issue4 =
      new IssueTree
      ("TEST-4", "top", "trent", 1L, "1", 8 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/13"), null, 4, false);
    IssueTree issue9 =
      new IssueTree
      ("TEST-9", "issue", "ken", 1L, "1", 20 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/05"), null, 3, false);
    IssueTree issue11 =
      new IssueTree
      ("TEST-11", "sub issue", "trent", 1L, "1", 4 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/15"), null, 3, true);
    IssueTree issue12 =
      new IssueTree
      ("TEST-12", "sub issue", "ken", 1L, "1", 3 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/06/01"), null, 2, false);
    IssueTree issue13 =
      new IssueTree
      ("TEST-13", "after TEST-1,2", "trent", 1L, "1", 10 * 3600, 1 * 3600,
       null, null, 2, true);
    IssueTree issue13_1 =
      new IssueTree
      ("TEST-13-1", "sub of TEST-13", "trent", 1L, "1", 10 * 3600, 1 * 3600,
       null, null, 9, false);
    IssueTree issue14 =
      new IssueTree
      ("TEST-14", "dependant issue", "trent", 1L, "1", 16 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/06/01"), null, 4, false);
    IssueTree issue15 =
      new IssueTree
      ("TEST-15", "dependant issue", "trent", 1L, "1", 4 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/07"), null, 5, false);
    IssueTree issue16 =
      new IssueTree
      ("TEST-16", "some issue", "trent", 1L, "1", 12 * 3600, 1 * 3600,
       SLASH_DATE.parse("2005/04/15"), null, 6, false);

    IssueTree[] manyIssues = {
      issue_6, issue_5, issue_4, issue_3, issue_2, issue_1, issue0, issue1,
      issue2, issue4, issue9, issue11, issue12, issue13, issue13_1,
      issue14, issue15, issue16
    };

    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, "trent", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "trent"), hourList);
           
    hourList = new ArrayList();
    hourList.add(new TeamHours(1L, 1L, "ken", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "ken"), hourList);

    hourList = new ArrayList();
    hourList.add(new TeamHours(2L, 1L, "brent", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "brent"), hourList);

    hourList = new ArrayList();
    hourList.add(new TeamHours(3L, 1L, "fred", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "fred"), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, userWeeklyHours);

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);


    // print out single-user table schedule & Gantt chart
    {
      String userPerson = "ken";
      Long userTeam = 1L;
      Teams.AssigneeKey userKey = new Teams.AssigneeKey(userTeam, userPerson);

      List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(userKey);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);

      out.println("<br><br>");
      out.println("Tree for " + userKey + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, userKey, false, graph));

    }





    out.println("<p>Adding dependency relationships...<p>");

    issue_6.addDependent(issue_4);
    issue_5.addDependent(issue_3);
    issue_4.addDependent(issue_3);
    issue_4.addDependent(issue_1);
    issue_3.addDependent(issue_1);
    issue_2.addDependent(issue_1);
    issue_1.addDependent(issue0);
    issue0.addDependent(issue1);
    issue0.addDependent(issue12);
    issue0.addDependent(issue14);
    issue1.addSubtask(issue11);
    issue1.addSubtask(issue12);
    issue1.addDependent(issue4);
    issue1.addDependent(issue13);
    issue2.addDependent(issue13);
    issue2.addSubtask(issue12);
    issue2.addSubtask(issue14);
    issue2.addSubtask(issue15);
    issue13.addSubtask(issue13_1);


    /**
       GRAPH OF SUBTASKS (+) AND BLOCKED TASKS (|)

. _6
.     _5
.  |----- _4
.      |----- _3
.          |- _3
.          |--------- _1
.              |----- _1
.                 _2
.                  |- _1
.                      |-  0
.                          |-  1
.                              +---------------------------- 11
.                              +-------------------------------- 12
.                          |------------------------------------ 12
.                          |-------------------------------------------- 14
.                                  2
.                                  +---------------------------- 12
.                                  +------------------------------------ 14
.                                  +---------------------------------------- 15
.                              |---------  4
.                              |------------------------------------ 13
.                                  |-------------------------------- 13
.                                                                     +_ 13_1
    **/

    graph = 
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);


    List branches1 = TimeScheduleWriter.findPredecessorBranches(issue1);
    out.println("<br><br>");
    out.println("All branches of TEST-1: ");
    out.println(branches1);

    out.println("<br><br>");
    out.println("Tree for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, true,
        new String[]{ issue1.getKey(), issue2.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Squished schedule for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (4, 0, true, false, true,
        new String[]{ issue1.getKey(), issue2.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Tree for issue 1, w/o resolved.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, false,
        new String[]{ issue1.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Schedule for trent.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForUser
       (1, 0, true, false, true, new Teams.AssigneeKey(1L, "trent"),
        false, graph));

    {
      Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "fred");
      out.println("<br><br>");
      out.println("Tree for " + user + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, user, false, graph));

    }

    {
      Teams.AssigneeKey[] users =
        { new Teams.AssigneeKey(1L, "fred"),
          new Teams.AssigneeKey(1L, "brent"),
          new Teams.AssigneeKey(1L, "ken"),
          new Teams.AssigneeKey(1L, "trent") };
      out.println("<p>");
      out.println("One-row schedule for all.<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out,
         sPrefs,
         TimeScheduleDisplayPreferences.createForUsers
         (1, 0, true, false, true, users, graph));

      out.println("<p>");
      out.println("Schedule for ken.<br>");
      Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "ken");
      TimeScheduleWriter.writeIssueTable
        (graph, out,
         sPrefs,
         TimeScheduleDisplayPreferences.createForUser
         (1, 0, true, false, true, user, false, graph));
    }

    // print out single-user table schedule
    {
      Teams.AssigneeKey[] users = { new Teams.AssigneeKey(1L, "ken") };
      List<TimeSchedule.IssueSchedule> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(users[0]);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      out.println("Schedule for " + Arrays.asList(users) + ".<br>");
      Map range =
        TimeScheduleLoader.weeklyHoursToRange
        (TimeScheduleLoader.teamToUserHours(userWeeklyHours));
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);
    }



    out.println("<xmp>");

    out.println
      ((issue1.totalTimeSpent() == 2 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 2 * 3600 + "; got " + issue1.totalTimeSpent() + ")");
    out.println
      ((issue1.totalEstimate() == 10 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 10 * 3600 + "; got " + issue1.totalEstimate() + ")");

    out.println
      ((issue1.totalTimeSpent() == 2 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 2 * 3600 + "; got " + issue1.totalTimeSpent() + ")");
    out.println
      ((issue1.totalEstimate() == 10 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 10 * 3600 + "; got " + issue1.totalEstimate() + ")");


    TimeSchedule.IssueSchedule sched13 =
      graph.getIssueSchedules().get(issue13.getKey());
    Calendar acal = Calendar.getInstance();
    acal = sched13.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 28 ? "pass" : "fail")
       + " (" + issue13.getKey() + " should start on Apr 22; got "
       + acal.get(Calendar.DATE) + ")");
    out.println
      ((acal.get(Calendar.HOUR_OF_DAY) == 8 ? "pass" : "fail")
       + " (" + issue13.getKey() + " should start at 14; got " +
       + acal.get(Calendar.HOUR_OF_DAY) + ")");

    TimeSchedule.IssueSchedule sched2 =
      graph.getIssueSchedules().get(issue2.getKey());
    boolean notBefore = !sched13.getBeginDate().before(sched2.getEndDate());
    out.println((notBefore ? "pass" : "fail")
                + " (" + issue13.getKey() + " starts after " + issue2.getKey() + " ends)");

    TimeSchedule.IssueSchedule sched14 =
      graph.getIssueSchedules().get(issue14.getKey());
    acal = sched14.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 12 ? "pass" : "fail")
       + " (" + issue14.getKey() + " should start on the 12th; got " +
       + acal.get(Calendar.DATE) + ")");

    TimeSchedule.IssueSchedule sched4 =
      graph.getIssueSchedules().get(issue4.getKey());
    acal = sched4.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 15 ? "pass" : "fail")
       + " (" + issue4.getKey() + " should start on the 14th; got " +
       + acal.get(Calendar.DATE) + ")");

    List branches0 = TimeScheduleWriter.findPredecessorBranches(issue0);
    out.println
      ((branches0.size() == 6 ? "pass" : "fail")
       + " (should be 6 branches preceding 0; got " + branches0.size() + ")");

    out.println("</xmp>");

    out.println();
    out.println("All branches of TEST-0: ");
    out.println(branches0);

  }


  private static Map<Teams.AssigneeKey,List<IssueTree>> createUserDetails
  (IssueTree[] issues, Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours) {

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = new HashMap();
    for (int i = 0; i < issues.length; i++) {
      addIssue(issues[i], userDetails, userWeeklyHours);
    }
    return userDetails;
  }
  /**
     Add issue to userDetails.
   */
  private static void addIssue
  (IssueTree issue,
   Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
   Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours) {

    Teams.AssigneeKey assignee = issue.getRawAssigneeKey();
    List<IssueTree> userIssues = userDetails.get(assignee);
    if (userIssues == null) {
      userIssues = new ArrayList<IssueTree>();
      userDetails.put(assignee, userIssues);
    }
    userIssues.add(issue);
  }





  /**
   * This reads from the database set up by jira-test-db.sql
   * and creates the output found in gantt-test-db.html
   */
  public static void integrationMain(String[] args) throws Exception {

    Connection conn = ForecastUtil.getConnection();

    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, new java.util.Date(), 2.0);
    String mainIssueKey = "FOURU-1002";
    IssueDigraph graph = TimeScheduleLoader.getGraph("", new String[]{ mainIssueKey }, new String[0], sPrefs, conn);

    // print out single-user time schedule
    {
      String user = "trent";
      System.out.println("Schedule for " + user + ".<br>");
      List<TimeSchedule.IssueSchedule> schedule = new ArrayList<TimeSchedule.IssueSchedule>();
      List<IssueTree> userIssueList = (List<IssueTree>) graph.getAssignedUserDetails().get(new Teams.AssigneeKey(null, user));
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true,
         new PrintWriter(System.out));
    }

    // print out that issue
    System.out.println("Gantt for " + mainIssueKey + ".<br>");
    TimeScheduleDisplayPreferences dPrefs =
      TimeScheduleDisplayPreferences.createForIssues
      (5, Calendar.MONTH, true, false, false, new String[]{ mainIssueKey }, false, graph);

    TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(System.out), sPrefs, dPrefs);
    
    
    // now let's load and schedule everything
    
    graph = TimeScheduleLoader.getEntireGraph(sPrefs, conn);
    
    // show that the first team has a lot of work while the second putters around.
    System.out.println("Gantt for team overloaded and underloaded teams.<br>");
    dPrefs =
      TimeScheduleDisplayPreferences.createForTeam
      (5, 0, true, false, false, new Long(20100), false, graph);
    TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(System.out), sPrefs, dPrefs);

    dPrefs =
      TimeScheduleDisplayPreferences.createForTeam
      (5, 0, true, false, false, new Long(20101), false, graph);
    TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(System.out), sPrefs, dPrefs);
    
  }



}

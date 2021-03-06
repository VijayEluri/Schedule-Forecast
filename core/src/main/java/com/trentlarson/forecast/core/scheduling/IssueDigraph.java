package com.trentlarson.forecast.core.scheduling;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.trentlarson.forecast.core.scheduling.TimeSchedule.IssueWorkDetail;

public class IssueDigraph {

  private Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules;
  private Map<Teams.AssigneeKey,List<IssueTree>> assignedUserDetails;
  private Map<Teams.UserTimeKey,List<IssueTree>> timeUserDetails;
  private Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers;
  private Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursAvailable;
  private Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursLeft;

  private TimeScheduleCreatePreferences prefs;
  private Date loadedDate = new Date();
  private int maxPriority = -1;
  
  /**
     @param issueSchedules_ maps issue key to IssueSchedule object
     @param userDetails_ maps user key to IssueDetail List
  */
  public IssueDigraph
  (Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules_,
   Map<Teams.AssigneeKey,List<IssueTree>> assignedUserDetails_,
   Map<Teams.UserTimeKey,List<IssueTree>> timeUserDetails_,
   Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers_,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursAvailable_,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursLeft_,
   TimeScheduleCreatePreferences prefs_) {

    this.issueSchedules = issueSchedules_;
    this.assignedUserDetails = assignedUserDetails_;
    this.timeUserDetails = timeUserDetails_;
    this.assigneeToAllocatedUsers = assigneeToAllocatedUsers_;
    this.userWeeklyHoursAvailable = userWeeklyHoursAvailable_;
    this.userWeeklyHoursLeft = userWeeklyHoursLeft_;
    this.prefs = prefs_;
  }

  /**
     @return a Map from issue pkey to TimeSchedule.IssueSchedule object for it
  */
  public Map<String,TimeSchedule.IssueSchedule<IssueTree>> getIssueSchedules() {
    return issueSchedules;
  }
  public TimeSchedule.IssueSchedule<IssueTree> getIssueSchedule(String key) {
    return getIssueSchedules().get(key);
  }

  public IssueTree getIssueTree(String key) {
    return (IssueTree) getIssueSchedule(key).getIssue();
  }

  /**
     @return a Map from user to priority-sequential List of
     IssueWorkDetail elements originally assigned to them

     See also timeUserDetails.
   */
  public Map<Teams.AssigneeKey,List<IssueTree>> getAssignedUserDetails() {
    return assignedUserDetails;
  }

  /**
     @return a Map from user to priority-sequential List of
     IssueWorkDetail elements assigned to them for purposes of
     scheduling

     This may be different from the assignedUserDetails because the
     hour allocations often go to individuals and the team information
     is ignored.  (Sometimes it could go the other way: the time is
     allocated to a whole team and the user information is ignored,
     though that is not implemented at the time of writing this
     comment.)
  */
  public Map<Teams.UserTimeKey,List<IssueTree>> getTimeUserDetails() {
    return timeUserDetails;
  }

  /**
     @return the user which has time allotments that may be used for scheduling
  */
  public Teams.UserTimeKey getAllocatedUser(Teams.AssigneeKey assignee) {
    return assigneeToAllocatedUsers.get(assignee);
  }


  /**
     @return a Map from user to weekly working hours originally scheduled
  */
  public Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> getUserWeeklyHoursAvailable() {
    return userWeeklyHoursAvailable;
  }

  /**
     @return a Map from user to weekly working hours that are left after scheduling
  */
  public Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> getUserWeeklyHoursLeft() {
    return userWeeklyHoursLeft;
  }

  public Date getLoadedDate() {
    return loadedDate;
  }
  /**
   * Return the maximum priority found in all the issues, calculating it if this is the first call.
   * 
   * Remember: priorities are 1-based.  If no priorities exist, a -1 is returned.
   * 
   * @see IssueWorkDetail#getPriority()
   */
  public int getMaxPriority() {
    if (maxPriority == -1) {
      for (Iterator<TimeSchedule.IssueSchedule<IssueTree>> iter = getIssueSchedules().values().iterator(); iter.hasNext(); ) {
        TimeSchedule.IssueSchedule<IssueTree> schedule = iter.next();
        if (maxPriority < schedule.getIssue().getPriority()) {
          maxPriority = schedule.getIssue().getPriority();
        }
      }
    }
    return maxPriority;
  }
  public TimeScheduleCreatePreferences getTimeScheduleCreatePreferences() {
    return prefs;
  }
  public String toString() {
    return "\n<ol>\n" + treeString() + "\n</ol>";
  }

  public String treeString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<String> i = getIssueSchedules().keySet().iterator(); i.hasNext(); ) {
      String key = i.next();
      sb.append(getIssueTree(key).treeString());
    }
    return sb.toString();
  }

  /* unused (and loops through all issues when called... why did I write this?)
  public Date findMaxEndDate(Date max) {
    for (Iterator<TimeSchedule.IssueSchedule<IssueTree>> iter = getIssueSchedules().values().iterator(); iter.hasNext(); ) {
      TimeSchedule.IssueSchedule<IssueTree> schedule = iter.next();
      if (max.compareTo(schedule.getAdjustedEndCal().getTime()) < 0) {
        max = schedule.getAdjustedEndCal().getTime();
      }
    }
    return max;
  }
  */

  public void reschedule() {
    
    // REFACTOR with TimeScheduleLoader.schedulesForUserIssues (lots of overlap)

    TimeScheduleLoader.UserDetailsAndHours newTimeDetails = 
      TimeScheduleLoader.adjustDetailsForAssignedHours
      (getAssignedUserDetails(), getUserWeeklyHoursAvailable(),
       getTimeScheduleCreatePreferences().getStartTime());

    Map<String,List<IssueTree>> issuesFromString =
      TimeScheduleLoader.createMapFromAssigneeKeyStringToUserIssues(newTimeDetails.timeDetails);

    // clone the hourly data (since it's modified later)
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey2 = new HashMap<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours>();
    for (Teams.UserTimeKey user : newTimeDetails.hours.keySet()) {
      weeklyHoursFromKey2
        .put(user,
             (TimeSchedule.WeeklyWorkHours) newTimeDetails.hours.get(user).clone());
    }

    Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursFromString =
      TimeScheduleLoader.createMapFromUserTimeKeyStringToWeeklyHours(weeklyHoursFromKey2);

    Map<String,TimeSchedule.IssueSchedule<IssueTree>> newSchedules =
      TimeSchedule.schedulesForUserIssues
      (issuesFromString, weeklyHoursFromString,
       getTimeScheduleCreatePreferences().getStartTime(),
       getTimeScheduleCreatePreferences().getTimeMultiplier());

    this.issueSchedules = newSchedules;
    this.userWeeklyHoursAvailable = newTimeDetails.hours;
    this.userWeeklyHoursLeft = weeklyHoursFromKey2;
  }

}


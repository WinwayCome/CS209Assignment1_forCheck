import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], dateFormat.parse(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            System.out.println("Read Error");
            e.printStackTrace();
        }
        catch(ParseException e)
        {
            System.out.println("Date Error");
            throw new RuntimeException(e);
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        return courses.stream().collect(Collectors.groupingBy(Course::getInstitution, TreeMap::new, Collectors.summingInt(Course::getParticipants)));
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        return courses.stream().map(InstSub_Int::new).collect(Collectors.groupingBy(InstSub_Int::getName, Collectors.summingInt(InstSub_Int::getSum)))
                .entrySet().stream().map(InstSub_Int::new).sorted(Comparator.comparing(InstSub_Int::getSum, Comparator.reverseOrder()).thenComparing(InstSub_Int::getName)).collect(Collectors.toMap(InstSub_Int::getName, InstSub_Int::getSum, (k,v)->v, LinkedHashMap::new));
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        List<Name_Course> name_courseStream = courses.stream().filter(x->x.getInstructors() != null && x.getTitle() != null).flatMap(x -> x.getInstructors().contains(",") ?
                Arrays.stream(x.getInstructors().split(", ", -1)).map(e -> new Name_Course(e, x.getTitle(), false)) :
                Stream.of(new Name_Course(x.getInstructors(), x.getTitle(), true))).toList();
        return Stream.concat(name_courseStream.stream().filter(Name_Course::isSelf).collect(Collectors.groupingBy(Name_Course::getName, Collectors.mapping(Name_Course::getCourse, Collectors.toSet()))).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x->x.getValue().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))).entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, x->new InstEr_Course(x.getValue(), true))).entrySet().stream(), name_courseStream.stream().filter(x->!x.isSelf()).collect(Collectors.groupingBy(Name_Course::getName, Collectors.mapping(Name_Course::getCourse, Collectors.toSet()))).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x->x.getValue().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))).entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, x->new InstEr_Course(x.getValue(), false))).entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, x -> {
            List<List<String>> link = new ArrayList<>();
            if(x.getValue().isType())
            {
                link.add(x.getValue().getCourses());
                link.add(new ArrayList<>());
            }
            else{
                link.add(new ArrayList<>());
                link.add(x.getValue().getCourses());
            }
            return link;
        }, (a, b)->{
            List<List<String>> link = new ArrayList<>();
            if(a.get(0).size() != 0)
            {
                link.add(a.get(0));
                link.add(b.get(1));
            }
            else{
                link.add(b.get(0));
                link.add(a.get(1));
            }
            return link;
        }));
        
    }
    //4
    public List<String> getCourses(int topK, String by) {
        switch(by)
        {
            case "hours":
                return courses.stream().sorted(Comparator.comparing(Course::getTotalHours, Comparator.reverseOrder())).map(Course::getTitle).distinct().limit(topK).collect(Collectors.toList());
            case "participants":
                return courses.stream().sorted(Comparator.comparing(Course::getParticipants, Comparator.reverseOrder())).map(Course::getTitle).distinct().limit(topK).collect(Collectors.toList());
        }
        return null;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        return courses.stream().filter(x -> x.getSubject().matches("(?i).*" + courseSubject + ".*") && x.getPercentAudited() >= percentAudited && x.getTotalHours() <= totalCourseHours).map(Course::getTitle).distinct().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        return courses.stream().collect(Collectors.groupingBy(Course::getNumber, Collectors.toSet())).entrySet().stream().map(
                x->{
                    OptionalDouble o_age = x.getValue().stream().mapToDouble(Course::getMedianAge).average();
                    OptionalDouble o_male = x.getValue().stream().mapToDouble(Course::getPercentMale).average();
                    OptionalDouble o_deg = x.getValue().stream().mapToDouble(Course::getPercentDegree).average();
                    return new AVG(x.getKey(), x.getValue().stream().collect(Collectors.collectingAndThen(Collectors.reducing(BinaryOperator.maxBy(Comparator.comparing(Course::getLaunchDate))), y-> y.map(Course::getTitle).orElse(null))), age, gender*100, isBachelorOrHigher*100, o_age.isPresent()? o_age.getAsDouble():0D, o_male.isPresent()? o_male.getAsDouble():0D, o_deg.isPresent()? o_deg.getAsDouble():0D);
                }).sorted(Comparator.comparing(AVG::getRecommend).thenComparing(AVG::getTitle)).map(AVG::getTitle).distinct().limit(10).collect(Collectors.toList());
    }

}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
    
    public String getInstitution()
    {
        return institution;
    }
    
    public String getNumber()
    {
        return number;
    }
    
    public Date getLaunchDate()
    {
        return launchDate;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public String getInstructors()
    {
        return instructors;
    }
    
    public String getSubject()
    {
        return subject;
    }
    
    public int getYear()
    {
        return year;
    }
    
    public int getHonorCode()
    {
        return honorCode;
    }
    
    public int getParticipants()
    {
        return participants;
    }
    
    public int getAudited()
    {
        return audited;
    }
    
    public int getCertified()
    {
        return certified;
    }
    
    public double getPercentAudited()
    {
        return percentAudited;
    }
    
    public double getPercentCertified()
    {
        return percentCertified;
    }
    
    public double getPercentCertified50()
    {
        return percentCertified50;
    }
    
    public double getPercentVideo()
    {
        return percentVideo;
    }
    
    public double getPercentForum()
    {
        return percentForum;
    }
    
    public double getGradeHigherZero()
    {
        return gradeHigherZero;
    }
    
    public double getTotalHours()
    {
        return totalHours;
    }
    
    public double getMedianHoursCertification()
    {
        return medianHoursCertification;
    }
    
    public double getMedianAge()
    {
        return medianAge;
    }
    
    public double getPercentMale()
    {
        return percentMale;
    }
    
    public double getPercentFemale()
    {
        return percentFemale;
    }
    
    public double getPercentDegree()
    {
        return percentDegree;
    }
}
class InstSub_Int
{
    String name;
    int sum;
    public InstSub_Int(Course course) {
        this.name = course.getInstitution() + "-" + course.getSubject();
        this.sum = course.getParticipants();
    }
    public InstSub_Int(Map.Entry<String, Integer> course)
    {
        this.name = course.getKey();
        this.sum = course.getValue();
    }
    public String getName()
    {
        return name;
    }
    
    public int getSum()
    {
        return sum;
    }
}

class InstEr_Course{
    boolean type;
    List<String> courses;
    public InstEr_Course(List<String> c, boolean type)
    {
        this.type = type;
        this.courses = c;
    }
    
    public boolean isType()
    {
        return type;
    }
    
    public List<String> getCourses()
    {
        return courses;
    }
}

class Name_Course
{
    String name;
    String course;
    boolean self;
    public Name_Course(String name, String course, boolean type){
        this.name = name;
        this.course = course;
        this.self = type;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getCourse()
    {
        return course;
    }
    
    public boolean isSelf()
    {
        return self;
    }
}
class Course_Hour
{
    String course;
    Double hour;
    public Course_Hour(Map.Entry<String, Double> m)
    {
        this.course = m.getKey();
        this.hour = m.getValue();
    }
    
    public String getCourse()
    {
        return course;
    }
    
    public Double getHour()
    {
        return hour;
    }
}
class Number_Course
{
    String number;
    String course;
    Date date;
    public Number_Course(String number, String course, Date date)
    {
        this.number = number;
        this.course = course;
        this.date = date;
    }
}
class AVG
{
    String number;
    String title;
    double recommend;
    public AVG(String number, String title, double age, double male, double deg,  double avg_age, double avg_male, double avg_deg)
    {
        this.number = number;
        this.title = title;
        this.recommend = Math.pow(age-avg_age,2)+Math.pow(male-avg_male, 2)+Math.pow(deg - avg_deg, 2);
    }
    
    public String getNumber()
    {
        return number;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public double getRecommend()
    {
        return recommend;
    }
}
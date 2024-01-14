create table Groups
(
    GroupId   Int32,
    GroupName Text,

    primary key (GroupId)
);

create table Students
(
    StudentId   Int32,
    StudentName Text,
    GroupId     Int32,

    primary key (StudentId)
);

create table Courses
(
    CourseId   Int32,
    CourseName Text,

    primary key (CourseId)
);

create table Lecturers
(
    LecturerId   Int32,
    LecturerName Text,

    primary key (LecturerId)
);

create table Plan
(
    GroupId    Int32,
    CourseId   Int32,
    LecturerId Int32,

    primary key (GroupId, CourseId, LecturerId)
);

create table Marks
(
    StudentId Int32,
    CourseId  Int32,
    Mark      Int32,

    primary key (StudentId, CourseId)
);
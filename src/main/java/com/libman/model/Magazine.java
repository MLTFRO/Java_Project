package com.libman.model;

public class Magazine extends Document {
        private Integer number;
        public enum Periodicity {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        BIMONTHLY,
        QUARTERLY,
        YEARLY
        }
        private Periodicity periodicity;

        public Magazine(String title, String author, String genre, int number, Periodicity periodicity){
            super(title, author, genre);
            this.number = number;
            this.periodicity = periodicity;
        }

        public Magazine(){
            super(null, null, null);
            this.number = null;
            this.periodicity = null;
        }

        public int getNumber(){
            return(this.number);
        }

        public Periodicity getPeriodicity(){
            return(this.periodicity);
        }

        public void setNumber(int number){
            this.number = number;
        }
        public void setPeriodicity(Periodicity periodicity){
            this.periodicity = periodicity;
        }
}
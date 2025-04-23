#!/usr/bin/perl

while(<>) {
        s/(Build|Сборка) (\d+)/sprintf("%s %d",$1, $2+1)/e; 
        print;
}




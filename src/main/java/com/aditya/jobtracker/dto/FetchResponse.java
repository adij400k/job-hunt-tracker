package com.aditya.jobtracker.dto;

// How many jobs Adzuna sent, and how many of them were new.
public record FetchResponse(int found, int added) {
}

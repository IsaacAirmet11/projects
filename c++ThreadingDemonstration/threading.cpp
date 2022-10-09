#include <iostream>
#include <thread>
#include <mutex>
#include <vector>
#include <string>
#include <cmath>
 
/* Global variables */
int thread_count;
int bin_count;
float min_meas;
float max_meas;
float bin_size;
int data_count;
std::vector<float> data_array;
std::vector<float> bin_size_checker;
std::vector<std::int32_t> bin_array;
std::vector<std::vector<int32_t>> thread_data_indicies;
std::mutex my_mutex;
 
/* Parallel Functions */
void *Partial_Histogram(long rank);

/*
Notes: To answer the question within this question, differeing the thread count has the potential to change the results if the amount of data points is not
evenly divisible by the thread count. Simply deviding the data point count by the thread count and thinking that will be a given threads work size is wrong 
when those two numbers do not devide evenly. To fix this issue, before the threads would get to work in Partial_Histogram, I iterated over the data array 
and assigned each value to a thread using a 2D vecotor. Each thread rank was an index leading to another vector of indicies which were theirs to work through 
in the data array. To solve this problem as a whole (and as already hinted to above) I implemented threads in such a way that each thread got a portion of the 
data array to work with. Each thread would sort it's portion of the array into a local histogram, and then afterwards update the global histogram by adding 
the local histogram bin values to it. I used mutex to (lock and unlock) to make sure that only one thread was impacting the global histogram at any given point,
preventing possible errors from occuring as threads would try to update the global histogram at the same time.
*/
 
int main (int argc, char* argv[]) {
    /* Validate there are enough parameters provided */
    if (argc != 6) {
        printf("Invalid Parameters: <number of threads> <bin count> <min meas> <max meas> <data count>\n");
        return 3;
    }
 
    /* Assign values from parameters given */
    long thread;
    thread_count = atoi(argv[1]);
    bin_count = atoi(argv[2]);
    min_meas = std::stof(argv[3]);
    max_meas = std::stof(argv[4]);
    data_count = atoi(argv[5]);
    bin_size = fabs(max_meas - min_meas) / bin_count;

    if (thread_count > data_count) {
        printf("More threads than data points. Please reduce thread count.\n");
        return 3;
    }
 
    /* Create random float value array*/
    srand(100);
    for (int i = 0; i < data_count; i++) {
        data_array.push_back(min_meas + static_cast <float> (rand()) / ( static_cast <float> (RAND_MAX / (max_meas - min_meas))));
    }
   
    /* Initalize all values of the bin array to 0 for future adding*/
    for (int i = 0; i < bin_count; i++) {
        bin_array.push_back(0);
    }

    /* Create array of min/max bin values to compare random floats against later on*/
    for (int i = 0; i <= bin_count; i++) {
        bin_size_checker.push_back(min_meas + (bin_size * i));
    }

    /* Create 2D vector for assigning threads their values to work on*/
    for (int i = 0; i < thread_count; i++) {
        thread_data_indicies.push_back(std::vector<int32_t>());
    }

    /* Assign values to each thread (thread rank is index)*/
    int y = 0;
    for (int i = 0; i < data_count; i++) {
        y = i;
        while (y >= thread_count) y -= thread_count;
        thread_data_indicies[y].push_back(i);
    }
 
    /* Create threads and later join them */
    std::thread thread_handles[thread_count];
    for (thread = 0; thread < thread_count; thread++) {
        thread_handles[thread] = std::thread(Partial_Histogram, thread);
    }
 
    for (thread = 0; thread < thread_count; thread++) {
        thread_handles[thread].join();
    }
 
    /* Create final strings showing results */
    std::string bin_maxes_str = "bin_maxes = ";
    std::string bin_count_str = "bin_counts = ";
    for (int i = 0; i < bin_count; i++) {
        bin_maxes_str += " " + std::to_string(bin_size_checker[i + 1]);
        bin_count_str += " " + std::to_string(bin_array[i]);
    }
    std::cout << bin_maxes_str << "\n";
    std::cout << bin_count_str << "\n";
 
    return 0;
}
 
/* Function which partially computes bin values for portion of random value array based on thread rank */
void *Partial_Histogram(long rank) {
    /* Get thread specific variables and bin array set up */
    long my_rank = rank;
    std::vector<std::int32_t> my_bins (bin_count, 0);
 
    /* Increment proper bin for each value assigned to the given thread, watching for end cases */
    for (int i : thread_data_indicies[rank]) {
        if (data_array[i] == min_meas) {
            my_bins[0] += 1;
        } else if (data_array[i] == max_meas) {
            my_bins[bin_count - 1] += 1;
        } else {
            int j = 0;
            while (bin_size_checker[j] < data_array[i]) j++;
            my_bins[j - 1] += 1;
        }
    }

    /* Update global bins with local bin info */
    my_mutex.lock();
    for (int i = 0; i < bin_count; i++) {
        bin_array[i] += my_bins[i];
    }
    my_mutex.unlock();
 
    return NULL;
}
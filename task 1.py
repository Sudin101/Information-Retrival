import os
import re
import time
import random
import urllib.request

from bs4 import BeautifulSoup

# Crawl the relevant web pages and retrieve information about all available publications
def crawl(url):
   # Retrieve the HTML code from the URL
   html = urllib.request.urlopen(url).read()
   
   # Parse the HTML code
   soup = BeautifulSoup(html, 'html.parser')
   
   # Extract the data from the HTML code
   data = soup.find_all('div', class_='publication')
   
   # Initialize the list of publications
   publications = []
   
   # Loop through all publications
   for item in data:
       # Initialize the dictionary for the publication
       publication = {}
       
       # Extract the title
       title = item.find('span', class_='title').text
       publication['title'] = title
       
       # Extract the authors
       authors = item.find('span', class_='authors').text
       publication['authors'] = authors
       
       # Extract the publication year
       year = item.find('span', class_='year').text
       publication['year'] = year
       
       # Extract the link to the publication page
       link = item.find('a', class_='pub-title')['href']
       publication['link'] = link
       
       # Extract the link to the author's profile page
       profile = item.find('a', class_='author-name')['href']
       publication['profile'] = profile
       
       # Add the publication to the list
       publications.append(publication)
   
   # Return the list of publications
   return publications

# Schedule the crawler to run once per week
def schedule():
   # Get the current time
   now = time.time()
   
   # Set the next run time to be one week from now
   next_run = now + (7 * 24 * 60 * 60)
   
   # Schedule the crawler to run again
   time.sleep(next_run - now)

# Update the index with the new data
def update_index(publications):
   # Open the index file
   with open('index.txt', 'w') as file:
       # Loop through all publications
       for publication in publications:
           # Write the publication to the index file
           file.write(publication['title'] + '\n')
           file.write(publication['authors'] + '\n')
           file.write(publication['year'] + '\n')
           file.write(publication['link'] + '\n')
           file.write(publication['profile'] + '\n')

# Main function
def main():
   # Crawl the publications
   publications = crawl('https://www.coventry.ac.uk/research/research-directories/publications/')
   
   # Update the index
   update_index(publications)
   
   # Schedule the crawler to run again
   schedule()

# Run the main function
if __name__ == '__main__':
   main()

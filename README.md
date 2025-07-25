# static-site-generator

### What is the idea of this project?
Lets say you have created some static HTML pages and you want to generate a website from them. This project is a PoC how you might want to do it.
The main idea is to leverage the Cloudflare R2 and the host the static files there, because:
- It is cheap(compared to S3)
- It is fast(use Cloudflare CDN)
- It is easy to maintain

### How it is implemented?
This projects implemets 2 endpoints:
- POST /generate - takes a project name and target HTML files(might have CSS and JS files as well) and:
    - generates a Cloudflare R2 bucket with the project name(using the Cloudflare REST API)
    - attachst custom domain to the bucket, so to make it public and gives it name of <project-name>.deviliablog.com(using the Cloudflare REST API)
    - uploads the files to the bucket using AWS SDK for S3(all main object storage APIs are compatible with S3 API)
- DELETE /generate - delete the target bucket by first removing all it files and then deleting the bucket itself

This approach has some key things to mention:
- By using this approach you offload a lot of the heavy lifting to the Cloudflare, so you don't need to worry about scaling, performance, etc.
- I think there is a limit of 1000 buckets per account, but I think you can make a request to increase it if you need more(because in this implemntation there is 1 bucket per 1 website).
- I believe you can some type of Cloudflare Zero Trust Application to protect the bucket from public access, so you can use it for private projects as well.

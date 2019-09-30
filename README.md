# mootz

디렉토리/파일 구조를 읽어 웹 서비스하는 프로그램이다. 

홈페이지를 로컬 터미널 환경(vim, emacs)로 작성하기 위해 만들었다. 로컬환경에서 웹서버로 옮기는 부분은 mootz의 영역이 아니다. ([rootz](https://github.com/snailoff/rootz)를 clojure 로 포팅함)


# install

1. [leiningen](https://leiningen.org) 설치.
2. `git clone https://github.com/snailoff/mootz`
3. `cd mootz`
4. `lein ring server` 

# rule

* `mootz/resources/public/root` 가 최상위 경로가 된다.
* 디렉토리는 directory list에 모두 표현된다.
* 확장자가 없는 파일만 file list 에 표현된다. 
* `_` 파일은 디렉토리의 기본 페이지이며 파일리스트에서 리스트되지 않는다.


# extension

* markdown (https://github.com/yogthos/markdown-clj)  
* images
* image

# tip

### deploy
로컬의 텍스트 파일을 옮기기위해 fswatch(https://github.com/emcrisostomo/fswatch)를 사용하는 예제.
<pre><code>
# fswatch를 설치후 아래의 스크립트를 실행하면 로컬의 변경사항을 실시간에 가깝게 웹서버에 반영할 수 있다.

/usr/local/bin/fswatch -e '\\.swp$' -o /local/mootz/path/resources/public/root | while read f; do rsync -rave "ssh -i ~/.ssh/yourpemfile.pem" --delete --exclude "*.swp" /local/mootz/path/resources/public/root account@domain:~/server/mootz/path/resources/public; done
</code></pre>








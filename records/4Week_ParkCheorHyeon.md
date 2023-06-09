## Title: [4Week] 박철현

### 미션 요구사항 분석 & 체크리스트

---
[4주차 미션 페이지](https://wiken.io/ken/12201#4주차)

### 4주차 미션 요약
- [x] (필수미션) 네이버클라우드플랫폼을 통한 배포, 도메인, HTTPS 까지 적용
  - [배포 결과](www.codelike.shop)
- [x] (필수미션) 내가 받은 호감리스트에서 각 성별별 필터링한 호감 표시 결과 보기
- [x] (선택미션) 내가 받은 호감리스트에서 각 호감사유별 필터링한 호감 표시 결과 보기
- [x] (선택미션) 내가 받은 호감리스트에서 정렬 결과 보기
  - [x] 최신순(기본)
  - [x] 날짜순
  - [x] 인기 많은 순
  - [x] 인기 적은 순
  - [x] 성별순
    - [x] 여성 -> 남성 순 정렬
    - [x] 2순위 조건으로는 최신순(여1,남1,여2,남2 순 표시=> 여2, 1, 남 2, 1 순)
  - [x] 호감사유 순
    - [x] 외모, 성격, 능력 순
    - [x] 2순위 정렬 조건으로는 최신순(외모, 능력, 외모, 성격 => 외모(제일 늦게 표시), 외모, 성격, 능력)
- [x] (선택미션) 젠킨스를 통해 main 브랜치 커밋 이벤트 발생 시 자동 배포 구현
---

**[접근 방법]**
<br>
- 구현 전
  - 목표 수립 : 필수 미션 구현, 젠킨스 제외 나머지 미션 모두 구현 성공하기
  - 선행 작업 : 그램그램 코드 추가 및 이해
    -> 강사님 코드 기반 리팩토링, 다국어 기능 추가, toList HTML 파일 추가
    -> 다국어 코드 메서드 내 사용된 클래스 등 이해는 못함..

- 구현 중
  - 시행착오
    1) LikablePersonList를 가져올 때 gender 속성까지 한번에 비교하여 가져오려 시도
      - 생각한  코드 : findByToInstaMemberIdAndToInstaMember_gender
      - 쿼리문을 보니 join을 다 하고 where절에 gender 확인 => select로 가져올 속성에 gender가 없으니 불가능한 것을 깨달음
      - 결국 리스트를 가져오고, stream 활용하여 성별 필터링 구현
    
    2) stream 활용의 미숙함
      - 필터링, 정렬 기준 코드를 짤 때 Stream이 다소 미숙했다.
      - 인터넷  찾아가며 맞게 짠 것 같은데 계속 빨간줄 떠서 GPT에게 코드 수정을 받음
      - 무료 버전이라 그런지 빨간 오류 계속 뜨기도 해서 다르게 바꿔보고 이것저것 시도..
      - 빨간줄 뜬 부분은 "인기순 정렬" 부분이였는데, 정렬하고 동일한 인기 있는 사람 있을까봐 
        최신순으로 하려했는데 조건에 없어서 그냥 최신순은 제외했더니 미션 구현 성공
        - .sorted(Comparator.comparingInt(a -> a.**getFromInstaMember()**.getToLikeablePeople().size()).thenComparing())
        - 위의 getFrom~ 빨간색..
- 구현 결과 : 필수 미션 100% 구현, 선택 미션 100% 구현
  - 필수 미션 : Stream 활용하여 구현
  - 선택 미션
    - Stream 활용
    - (젠킨스) 강사님 강의보며 변형해가며 진행
      - ACG 설정 8080 포트 열어두기
      - Web hook 설정
      - 빌드가 젠킨스 프로젝트 폴더에서 될 것 같아 젠킨스 프로젝트 폴더 내 Dockerfile 생성
      - 젠킨스 파이프라인 수정 및 적용
        - h2 메모리모드라 테스트 DB설정 따로 하지 않음

- 접근 방법
  - 제일 처음에는 위에 생각한 코드로 언급한 것 처럼 findBy로 한번에 해결하고자 함
  - 하지만 불가능 함을 깨닫고 Stream 활용
  - 서비스로 아에 넘기고 결과를 받아서 model에 넣을까 했으나, 데이터를 새로 받아오는 것이 아닌
    가공하는 수준이라 컨트롤러가 해도 괜찮을 것 같아 셀렉트 박스 각각의 경우를 메서드로 만들어 적용
  - (젠킨스) 강사님 영상 참조하여 성공

**[특이사항]**

- 아쉬웠던 점
  - 스트림 활용의 미숙
    - 평소 수업만 따라가고, 강사님 코드보고 이해정도만 했고 처음 짜보는데, 많이 어려움
    - 스트림을 많이 써보면 좋을 것 같음. 리스트 정리를 스트림을 안써보려 했는데 더 어려웠던 것 같음.. 

- 리팩토링 시 추가 진행 방향
  - QueryDsl 활용 등 Steeam이 아닌 다른 풀이(?)
  - 테스트케이스 추가

- 궁금한점
  - 젠킨스가 리눅스가 아닌 도커인데(도커를 총괄하는 것이 아닌 컨테이너), 
    어떻게 이미지를 삭제하고 할 수 있는지.. 권한이 있는지 모르겠습니다.

**[리펙토링 결과]**
- Stream 방식의 코드를 조금 잘못 구현한 것을 확인하고 수정 
  - 기존 : likeablePerson 기본적으로 최신순인것을 모르고, 정렬 오래된 순을 그냥 리스트 반환
  - 변경 : 최신순일때 그냥 리스트 반환, 오래된 순일 경우 id 기준으로 오름차순 정렬 수정
- 강사님 코드로 테스트케이스 추가
**[느낀점]**
- 2번째 미션(3, 4주차)을 100% 다 구현한건 처음이라 뿌듯하기도 하고 이게 맞나 싶기도 하다.
- 스트림을 자주 활용하여 익숙해지고 싶다.
- 젠킨스 활용에 대해 미리 한번 해봤으니 수업때 조금 더 이해할 수 있을듯 함.
- 그램그램이 이제 끝인건가 하는 아쉬움이 있다. 
- 이번 미션 덕분에 스트림 sorted를 좀 더 공부할 수 있어 좋았다.
- QueryDSL을 공부하고 나서 리팩토링을 하고 싶고, DB부분을 조금 까먹었으니 다시 복습 후에 공부하고자 한다.
- 곧 팀 프로젝트인데 그램그램을 통해 학습한 내용을 잘 적용시켜 성공적으로 마무리 하고 싶다. 
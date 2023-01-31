#!/bin/bash

# author: keith cascio, 20100113

# http://en.wikipedia.org/wiki/List_of_Internet_top-level_domains
# http://www.regular-expressions.info/email.html "How to Find or Validate an Email Address"

# mysql 075504> describe samiam;
# +-------------+------------------------------+------+-----+---------------------+----------------+
# | Field       | Type                         | Null | Key | Default             | Extra          |
# +-------------+------------------------------+------+-----+---------------------+----------------+
# | ID          | int(10) unsigned             | NO   | PRI | NULL                | auto_increment |
# | Name        | varchar(80)                  | NO   |     | 0                   |                |
# | Email       | varchar(80)                  | NO   |     | 0                   |                |
# | Org         | varchar(150)                 | NO   |     | 0                   |                |
# | OS          | varchar(30)                  | NO   |     | 0                   |                |
# | Ver         | varchar(20)                  | NO   | MUL | 0                   |                |
# | IP          | varchar(15)                  | YES  |     | NULL                |                |
# | Datetime    | datetime                     | NO   |     | 0000-00-00 00:00:00 |                |
# | StatusEmail | tinyint(3) unsigned zerofill | YES  |     | 000                 |                |
# | idcountry   | tinyint(3) unsigned          | YES  |     | NULL                |                |
# +-------------+------------------------------+------+-----+---------------------+----------------+
# 10 rows

       gTLD='aero|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel'
      ccTLD='ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw'

      valid='[a-z].+[@].*[a-z][a-z].*[.]('"${gTLD}|${ccTLD}"')$'
   hardcore='[a-z0-9!#$%&'"'"'*+/=?^_`{|}~-]+(\.[a-z0-9!#$%&'"'"'*+/=?^_`{|}~-]+)*@([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+('"${gTLD}|${ccTLD}"')$'

significant='[a-z].+[@]..+[.]('"${gTLD}|${ccTLD}"')$'

 norepeater='([a-z0-9])\1+[@]([a-z0-9])\2+[.]('"${gTLD}|${ccTLD}"')$'

  nohomerow='^[asdfghjkl;]*(asd|sdf|dfg|fgh|ghj|hjk|jkl|kl;|;lk|lkj|kjh|jhg|hgf|gfd|fds|dsa)[asdfghjkl;]*[@][asdfghjkl;]+[.]('"${gTLD}|${ccTLD}"')$'
   noqwerty='^[qwertyuiop]*(qwe|wer|ert|rty|tyu|yui|uio|iop|poi|oiu|iuy|uyt|ytr|tre|rew|ewq)[qwertyuiop]*[@][qwertyuiop]+[.]('"${gTLD}|${ccTLD}"')$'

      noabc='^ab(c(d(e(fg?)?)?)?)?[@].*[.]('"${gTLD}|${ccTLD}"')$'

     nospam='spam'

# http://en.wikipedia.org/wiki/Metasyntactic_variable
METASYNTACTIC='foo|bar|baz|qux|quux|grault|garply|plugh|xyzzy'

nometasyntax="^(${METASYNTACTIC})[@]"

while getopts ":vn" opt; do
  case $opt in
    v  )  verbose='true';;
    n  )   dryrun='true';;
    \? ) echo "unknown option $opt = $OPTARG";;
  esac
done
shift $(($OPTIND - 1));

INPUT=`cat`
COUNT=$(wc -l <<< "${INPUT}")
echo "received ${COUNT} addresses" 1>&2
CORIG="${COUNT}"

function filter
{
  SAVED="${INPUT}"
  [[ "${1}" =~ '^no' ]] && reverse='v' || reverse=''
  INPUT=$(eval 'grep -Pi'"${reverse}"' "${'"${1}"'}" <<< "${INPUT}"')
  LESSER=$(wc -l <<< "${INPUT}")
  printf $"%-16s filter removed another %4d = %4d\n" "|${1}|" $(bc <<< "${COUNT} - ${LESSER}") "${LESSER}" 1>&2
  [ "${verbose}" ] && diff <(echo "${SAVED}") <(echo "${INPUT}") | sed -nre 's/^< //p' 1>&2
  COUNT="${LESSER}"
}

filter hardcore
filter significant
filter norepeater
filter nohomerow
filter noqwerty
filter noabc
filter nospam
filter nometasyntax

echo
printf $"%-16s  %02.2f%% removed   total %4d = %4d\n" "" $(bc <<< "scale=8; ((${CORIG} - ${COUNT})/${CORIG})*100") $(bc <<< "${CORIG} - ${COUNT}") "${COUNT}" 1>&2

[ "${dryrun}" ] || echo "${INPUT}"

